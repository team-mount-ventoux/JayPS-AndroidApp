package com.njackson;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

public class VirtualPebble {
    
    private static final String TAG = "PB-VirtualPebble";

    private static VirtualPebble _instance;
    private static Context _context;
    
    private int transID = 0;
    private PebbleKit.PebbleAckReceiver ackReceiver;
    private PebbleKit.PebbleNackReceiver nackReceiver;
    private final MessageManager messageManager = new MessageManager();
    
    public static void start(Context context) {
        _context = context;
        _instance = new VirtualPebble();
    }

    public static void sendDataToPebble(PebbleDictionary data) {
        _instance.messageManager.offer(data);
    }
    public static void sendDataToPebbleIfPossible(PebbleDictionary data) {
        _instance.messageManager.offerIfLow(data, 5);
    }
    public static void sendDataToPebble(PebbleDictionary data, boolean forceSend) {
        if (forceSend) {
            sendDataToPebble(data);
        } else {
            sendDataToPebbleIfPossible(data);
        }
    }

    public VirtualPebble() {
        
        new Thread(messageManager).start();
        
        // TODO: put VirtualPebble in a service and call unregisterReceiver(ackReceiver) in onDestroy
        
        ackReceiver = new PebbleKit.PebbleAckReceiver(Constants.WATCH_UUID) {
            @Override
            public void receiveAck(final Context context, final int transactionId) {
                messageManager.notifyAckReceivedAsync(transactionId);
            }
        };
        PebbleKit.registerReceivedAckHandler(_context, ackReceiver);

        nackReceiver = new PebbleKit.PebbleNackReceiver(Constants.WATCH_UUID) {
            @Override
            public void receiveNack(final Context context, final int transactionId) {
                messageManager.notifyNackReceivedAsync(transactionId);
            }
        };
        PebbleKit.registerReceivedNackHandler(_context, nackReceiver);
        
        PebbleKit.registerPebbleConnectedReceiver(_context, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
              _instance.messageManager.pebbleConnected();
            }
        });  
        PebbleKit.registerPebbleDisconnectedReceiver(_context, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
              //Log.d(TAG, "Pebble disconnected!");
            }
        });        
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
                            PebbleKit.sendDataToPebbleWithTransactionId(_context, Constants.WATCH_UUID, data, transID);
                        }

                        isMessagePending = Boolean.valueOf(true);
                    }
                }
            });
        }

        public void notifyAckReceivedAsync(int transactionId) {
            if (MainActivity.debug) Log.d(TAG, "notifyAckReceivedAsync("+transactionId+") transID:" + transID);
            removeMessageASync();
            consumeAsync();
        }

        public void notifyNackReceivedAsync(int transactionId) {
            if (MainActivity.debug) Log.d(TAG, "notifyNackReceivedAsync("+transactionId+") transID:" + transID);
            removeMessageASync();
            consumeAsync();
        }
        public void pebbleConnected() {
            if (MainActivity.debug) Log.d(TAG, "pebbleConnected");
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