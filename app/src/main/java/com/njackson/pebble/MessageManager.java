package com.njackson.pebble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.njackson.Constants;
import com.njackson.adapters.AdvancedLocationToNewLocation;
import com.njackson.adapters.NewLocationToPebbleDictionary;
import com.njackson.analytics.IAnalytics;
import com.njackson.application.PebbleBikeApplication;
import com.njackson.events.GPSServiceCommand.NewLocation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;

import fr.jayps.android.AdvancedLocation;

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

    private Thread _thisThread;

    private boolean debug = true;
    private Boolean _hasStarted = Boolean.valueOf(false);
    private boolean _isConnected = false;
    private int _skipped = 0;

    @Inject SharedPreferences _sharedPreferences;
    @Inject IAnalytics _parseAnalytics;

    public MessageManager(SharedPreferences preferences, Context context) {
        _sharedPreferences = preferences;
        debug = _sharedPreferences.getBoolean("PREF_DEBUG", false);
        _applicationContext = context;
        _thisThread = new Thread(this);
        _thisThread.start();

        setupPebbbleHandlers();
        ((PebbleBikeApplication) context).inject(this);
    }

    private void removeMessageASync() {
        synchronized (_hasStarted) {
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
    }

    private void setupPebbbleHandlers() {
        ackReceiver = new PebbleKit.PebbleAckReceiver(Constants.WATCH_UUID) {
            @Override
            public void receiveAck(final Context context, final int transactionId) {
                notifyAckReceivedAsync(transactionId);
                _isConnected = true;
            }
        };
        PebbleKit.registerReceivedAckHandler(_applicationContext, ackReceiver);

        nackReceiver = new PebbleKit.PebbleNackReceiver(Constants.WATCH_UUID) {
            @Override
            public void receiveNack(final Context context, final int transactionId) {
                notifyNackReceivedAsync(transactionId);
                _isConnected = true;
            }
        };
        PebbleKit.registerReceivedNackHandler(_applicationContext, nackReceiver);

        PebbleKit.registerPebbleConnectedReceiver(_applicationContext, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                _isConnected = true;
                pebbleConnected();
            }
        });
        PebbleKit.registerPebbleDisconnectedReceiver(_applicationContext, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                _isConnected = false;
                if (debug) Log.i(TAG, "Pebble disconnected!");
            }
        });
    }

    private void notifyAckReceivedAsync(int transactionId) {
        if (debug) Log.i(TAG, "notifyAckReceivedAsync("+transactionId+") transID:" + transID);
        removeMessageASync();
        consumeAsync();
    }

    private void notifyNackReceivedAsync(int transactionId) {
        if (debug) Log.i(TAG, "notifyNackReceivedAsync("+transactionId+") transID:" + transID);
        removeMessageASync();
        consumeAsync();
    }
    private void pebbleConnected() {
        if (debug) Log.i(TAG, "pebbleConnected transID:" + transID);
        removeMessageASync();
        consumeAsync();
    }

    private void consumeAsync() {
        if (debug) Log.v(TAG, "consumeAsync");
        synchronized (_hasStarted) {
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
                        if (debug) Log.i(TAG, "sendDataToPebble s:" + messageQueue.size() + " transID:" + transID + " " + data.toJsonString());
                            PebbleKit.sendDataToPebbleWithTransactionId(_applicationContext, Constants.WATCH_UUID, data, transID);
                        }

                        isMessagePending = Boolean.valueOf(true);
                    }
                }
            });
        }
    }

    @Override
    public void run() {
        synchronized (_hasStarted) {
            //SystemClock.sleep(7000); // uncomment to simulate the race condition
            Looper.prepare();
            messageHandler = new Handler();
        }
        _hasStarted = Boolean.valueOf(true);
        Looper.loop();
    }

    public boolean offer(final PebbleDictionary data) {
        final boolean success = messageQueue.offer(data);
        if (debug) {
            int s = messageQueue.size();
            if (s > 1) Log.i(TAG, "offer s:" + s);
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
                if (debug) Log.i(TAG, "offerIfLow s:" + s + ">" + sizeMax);
                if (_isConnected) {
                    _skipped++;
                    if (_skipped == 100) {
                        // only track 100th message
                        _parseAnalytics.trackSkippedMessage();
                    }
                }
                return false;
            }
            success = messageQueue.offer(data);
            if (debug) {
                if (s > 1) Log.i(TAG, "offerIfLow s:" + s + "<=" + sizeMax);
            }
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
    public void sendAckToPebble(int transactionId) {
        if (debug) Log.i(TAG, "sendAckToPebble("+transactionId+")");
        PebbleKit.sendAckToPebble(_applicationContext,transactionId);
    }

    @Override
    public void showSimpleNotificationOnWatch(String title, String text) {
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

    @Override
    public void sendMessageToPebble(String message) {
        showSimpleNotificationOnWatch("Pebble Bike", message);
    }

    @Override
    public void sendSavedDataToPebble(boolean isLocationServicesRunning, int units, float distance, long elapsedTime, float ascent, float maxSpeed) {

        // use AdvancedLocation and than NewLocation to use units conversion in AdvancedLocationToNewLocation

        AdvancedLocation advancedLocation = new AdvancedLocation();
        advancedLocation.setDistance(distance);
        advancedLocation.setElapsedTime(elapsedTime);
        advancedLocation.setAscent(ascent);
        advancedLocation.setMaxSpeed(maxSpeed);

        NewLocation newLocation = new AdvancedLocationToNewLocation(advancedLocation, 0, 0, units);

        PebbleDictionary dictionary = new NewLocationToPebbleDictionary(
                newLocation,
                isLocationServicesRunning,
                _sharedPreferences.getBoolean("PREF_DEBUG", false),
                _sharedPreferences.getBoolean("LIVE_TRACKING", false),
                Integer.valueOf(_sharedPreferences.getString("REFRESH_INTERVAL", String.valueOf(Constants.REFRESH_INTERVAL_DEFAULT))),
                255 // 255: no Heart Rate available
        );
        dictionary.addInt32(Constants.MSG_VERSION_ANDROID, Constants.VERSION_ANDROID);
        offer(dictionary);
    }
}
