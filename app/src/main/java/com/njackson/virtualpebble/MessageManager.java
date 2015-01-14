package com.njackson.virtualpebble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.njackson.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Manages a thread-safe message queue using a Looper worker thread to complete blocking tasks.
 */
public class MessageManager implements IMessageManager, Runnable {

    private static String TAG = "PB-MessageManager";

    private int transID = 0;
    public Handler messageHandler;
    private final BlockingQueue<PebbleDictionary> messageQueue = new LinkedBlockingQueue<PebbleDictionary>();
    private Boolean isMessagePending = false;
    private PebbleKit.PebbleAckReceiver ackReceiver;
    private PebbleKit.PebbleNackReceiver nackReceiver;

    private Context _applicationContext;

    public MessageManager(Context context) {
        _applicationContext = context;
    }

    private void removeMessageASync() {
        messageHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (isMessagePending) {
                    isMessagePending = Boolean.valueOf(false);
                    if (messageQueue.size() == 0) {
                        // if possible (?): bug
                        return;
                    }
                    messageQueue.remove();
                }
            }
        });
    }

    private void setupPebbbleHandlers() {
        ackReceiver = new PebbleKit.PebbleAckReceiver(Constants.WATCH_UUID) {
            @Override
            public void receiveAck(final Context context, final int transactionId) {
                notifyAckReceivedAsync(transactionId);
            }
        };
        PebbleKit.registerReceivedAckHandler(_applicationContext, ackReceiver);

        nackReceiver = new PebbleKit.PebbleNackReceiver(Constants.WATCH_UUID) {
            @Override
            public void receiveNack(final Context context, final int transactionId) {
                notifyNackReceivedAsync(transactionId);
            }
        };
        PebbleKit.registerReceivedNackHandler(_applicationContext, nackReceiver);

        PebbleKit.registerPebbleConnectedReceiver(_applicationContext, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                pebbleConnected();
            }
        });
        PebbleKit.registerPebbleDisconnectedReceiver(_applicationContext, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Log.d(TAG, "Pebble disconnected!");
            }
        });
    }

    private void notifyAckReceivedAsync(int transactionId) {
        Log.d(TAG, "notifyAckReceivedAsync("+transactionId+") transID:" + transID);
        removeMessageASync();
        consumeAsync();
    }

    private void notifyNackReceivedAsync(int transactionId) {
        Log.d(TAG, "notifyNackReceivedAsync("+transactionId+") transID:" + transID);
        removeMessageASync();
        consumeAsync();
    }
    private void pebbleConnected() {
        Log.d(TAG, "pebbleConnected");
        removeMessageASync();
        consumeAsync();
    }

    private void consumeAsync() {
        messageHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (isMessagePending) {
                    if (isMessagePending.booleanValue()) {
                        return;
                    }

                    synchronized (messageQueue) {
                        if (messageQueue.size() == 0) {
                            return;
                        }
                        transID = (transID + 1) % 256;
                        PebbleDictionary data = messageQueue.peek();
                        // if (MainActivity.debug) Log.d(TAG, "sendDataToPebble s:" + messageQueue.size() + " transID:" + transID + " " + data.toJsonString());
                        PebbleKit.sendDataToPebbleWithTransactionId(_applicationContext, Constants.WATCH_UUID, data, transID);
                    }

                    isMessagePending = Boolean.valueOf(true);
                }
            }
        });
    }

    @Override
    public void run() {
        setupPebbbleHandlers();
        Looper.prepare();
        messageHandler = new Handler();
        Looper.loop();
    }

    public boolean offer(final PebbleDictionary data) {
        final boolean success = messageQueue.offer(data);
        if (success) {
            consumeAsync();
        }

        return success;
    }

    public boolean offerIfLow(final PebbleDictionary data, int sizeMax) {
        boolean success = false;
        synchronized (messageQueue) {
            int s = messageQueue.size();
            if (s > sizeMax) {

                return false;
            }
            success = messageQueue.offer(data);
        }

        if (success) {
            consumeAsync();
        }

        return success;
    }

    @Override
    public void showWatchFace() {
        PebbleKit.startAppOnPebble(_applicationContext,Constants.WATCH_UUID);
    }

    @Override
    public void hideWatchFace() {
        PebbleKit.closeAppOnPebble(_applicationContext,Constants.WATCH_UUID);
    }

    @Override
    public void showSimpleNotificationOnWatch(String title, String text) {
        Log.d(TAG, "showSimpleNotificationOnWatch " + title + text);
        final Intent i = new Intent("com.getpebble.action.SEND_NOTIFICATION");
        final Map<String, String> data = new HashMap<String, String>();
        data.put("title", title);
        data.put("body", text);
        final JSONObject jsonData = new JSONObject(data);
        final String notificationData = new JSONArray().put(jsonData).toString();

        i.putExtra("messageType", "PEBBLE_ALERT");
        i.putExtra("sender", "Pebble Bike");
        i.putExtra("notificationData", notificationData);

        _applicationContext.sendBroadcast(i);
    }

}
