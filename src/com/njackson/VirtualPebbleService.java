package com.njackson;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.Service;
import android.content.IntentFilter;
import android.os.IBinder;
import org.json.JSONException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

public class VirtualPebbleService extends Service{
    
    private final String TAG = "PB-VirtualPebble";
    public static final String PEBBLE_DATA_EVENT = "PEBBLE_DATA_EVENT";
    public static final String INTENT_EXTRA_NAME = "PEBBLE_DATA";

    private int transID = 0;
    private PebbleKit.PebbleAckReceiver ackReceiver;
    private PebbleKit.PebbleNackReceiver nackReceiver;
    private final MessageManager messageManager = new MessageManager();
    private BroadcastReceiver _broadcastReceiver;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return START_STICKY;
    }

    @Override
    public void onCreate() {

        registerBroadcastReceiver();

        new Thread(messageManager).start();

        ackReceiver = new PebbleKit.PebbleAckReceiver(Constants.WATCH_UUID) {
            @Override
            public void receiveAck(final Context context, final int transactionId) {
                messageManager.notifyAckReceivedAsync(transactionId);
            }
        };
        PebbleKit.registerReceivedAckHandler(getApplicationContext(), ackReceiver);

        nackReceiver = new PebbleKit.PebbleNackReceiver(Constants.WATCH_UUID) {
            @Override
            public void receiveNack(final Context context, final int transactionId) {
                messageManager.notifyNackReceivedAsync(transactionId);
            }
        };
        PebbleKit.registerReceivedNackHandler(getApplicationContext(), nackReceiver);

        PebbleKit.registerPebbleConnectedReceiver(getApplicationContext(), new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                messageManager.pebbleConnected();
            }
        });
        PebbleKit.registerPebbleDisconnectedReceiver(getApplicationContext(), new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Log.d(TAG, "Pebble disconnected!");
            }
        });

        super.onCreate();
    }

    private void registerBroadcastReceiver() {

        _broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String jsonString = intent.getStringExtra(INTENT_EXTRA_NAME);
                Log.w(TAG,"Got Data:" + jsonString);
                try {
                    PebbleDictionary data = PebbleDictionary.fromJson(jsonString);
                    sendDataToPebble(data);
                }catch (JSONException e) {
                    Log.w(TAG,"Error decoding json data");
                }
            }
        };
        IntentFilter dataFilter = new IntentFilter(PEBBLE_DATA_EVENT);
        registerReceiver(_broadcastReceiver,dataFilter);
    }

    @Override
    public void onDestroy (){
        //unregisterReceiver(ackReceiver);
    }

    private void handleIntent(Intent intent) {

    }

    private void sendDataToPebble(PebbleDictionary data) {
        messageManager.offer(data);
    }
    private void sendDataToPebbleIfPossible(PebbleDictionary data) {
        messageManager.offerIfLow(data, 5);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
    * Manages a thread-safe message queue using a Looper worker thread to complete blocking tasks.
    */
    public class MessageManager implements Runnable {
        public Handler messageHandler;
        private final BlockingQueue<PebbleDictionary> messageQueue = new LinkedBlockingQueue<PebbleDictionary>();
        private Boolean isMessagePending = Boolean.valueOf(false);

        @Override
        public void run() {
            Looper.prepare();
            messageHandler = new Handler();
            Looper.loop();
        }

        public void consumeAsync() {
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
                            if (MainActivity.debug) Log.d(TAG, "sendDataToPebble s:" + messageQueue.size() + " transID:" + transID + " " + data.toJsonString());
                            PebbleKit.sendDataToPebbleWithTransactionId(getApplicationContext(), Constants.WATCH_UUID, data, transID);
                        }

                        isMessagePending = Boolean.valueOf(true);
                    }
                }
            });
        }

        public void notifyAckReceivedAsync(int transactionId) {
            Log.d(TAG, "notifyAckReceivedAsync("+transactionId+") transID:" + transID);
            removeMessageASync();
            consumeAsync();
        }

        public void notifyNackReceivedAsync(int transactionId) {
            Log.d(TAG, "notifyNackReceivedAsync("+transactionId+") transID:" + transID);
            removeMessageASync();
            consumeAsync();
        }
        public void pebbleConnected() {
            Log.d(TAG, "pebbleConnected");
            removeMessageASync();
            consumeAsync();
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

        public boolean offer(final PebbleDictionary data) {
            final boolean success = messageQueue.offer(data);
            if (MainActivity.debug) {
                int s = messageQueue.size();
                if (s > 1) Log.d(TAG, "offer s:" + s);
            }

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
                    if (MainActivity.debug) Log.d(TAG, "offerIfLow s:" + s + ">" + sizeMax);
                    return false;
                }
                success = messageQueue.offer(data);
                if (MainActivity.debug) {
                    if (s > 1) Log.d(TAG, "offerIfLow s:" + s + "<=" + sizeMax);
                }
            }
            
            if (success) {
                consumeAsync();
            }

            return success;
        }
    
    }
}