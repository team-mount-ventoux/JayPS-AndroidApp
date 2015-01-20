package com.njackson.pebble;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.njackson.Constants;
import com.njackson.adapters.NewLocationToPebbleDictionary;
import com.njackson.application.PebbleBikeApplication;
import com.njackson.events.PebbleService.NewMessage;
import com.njackson.events.status.PebbleStatus;
import com.njackson.events.status.GPSStatus;
import com.njackson.events.GPSService.NewLocation;
import com.njackson.events.LiveService.LiveMessage;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;


public class PebbleService extends Service {

    @Inject IMessageManager _messageManager;
    @Inject SharedPreferences _sharedPreferences;
    @Inject Bus _bus;

    private static final String TAG = "PB-PebbleService";
    private Thread _messageThread;

    private int _numberOfFriendsSentToPebble = 0;

    @Subscribe
    public void onNewLocationEvent(NewLocation newLocation) {
        if (newLocation.getTime() > 0) {
            PebbleDictionary dictionary = new NewLocationToPebbleDictionary(
                    newLocation,
                    true, // TODO(nic)
                    _sharedPreferences.getBoolean("PREF_DEBUG", false),
                    _sharedPreferences.getBoolean("LIVE_TRACKING", false),
                    Integer.valueOf(_sharedPreferences.getString("REFRESH_INTERVAL", "1000")),
                    newLocation.getHeartRate()
            );
            sendDataToPebble(dictionary);
        }
    }

    @Subscribe
    public void onGPSServiceState(GPSStatus event) {
        if(event.getState().compareTo(GPSStatus.State.STARTED) == 0) {
            _messageManager.showWatchFace();
        } else if (event.getState().compareTo(GPSStatus.State.STOPPED) == 0) {
            PebbleDictionary dictionary = new PebbleDictionary();
            dictionary.addInt32(Constants.STATE_CHANGED,Constants.STATE_STOP);
            sendDataToPebble(dictionary);
        }
    }

    @Subscribe
    public void onNewMessageEvent(NewMessage message) {
        _messageManager.showSimpleNotificationOnWatch("Pebble Bike", message.getMessage());
    }

    //TODO: needs tests
    @Subscribe
    public void onLiveMessage(LiveMessage msg) {
        Log.d(TAG, "onLiveMessage");

        PebbleDictionary dic = new PebbleDictionary();
        byte[] msgLiveShort = msg.getLive();
        boolean forceSend = false;
        String sending = "";
        if (_numberOfFriendsSentToPebble != msgLiveShort[0] || (5 * Math.random() <= 1)) {
            _numberOfFriendsSentToPebble = msgLiveShort[0];
            if (msg.getName0() != null) {
                dic.addString(Constants.MSG_LIVE_NAME0, msg.getName0());
            }
            if (msg.getName1() != null) {
                dic.addString(Constants.MSG_LIVE_NAME1, msg.getName1());
            }
            if (msg.getName2() != null) {
                dic.addString(Constants.MSG_LIVE_NAME2, msg.getName2());
            }
            if (msg.getName3() != null) {
                dic.addString(Constants.MSG_LIVE_NAME3, msg.getName3());
            }
            if (msg.getName4() != null) {
                dic.addString(Constants.MSG_LIVE_NAME4, msg.getName4());
            }
            sending += " MSG_LIVE_NAMEx"+msgLiveShort[0];
            forceSend = true;
        }
        dic.addBytes(Constants.MSG_LIVE_SHORT, msgLiveShort);
        for( int i = 0; i < msgLiveShort.length; i++ ) {
            sending += " msgLiveShort["+i+"]: "   + ((256+msgLiveShort[i])%256);
        }

        sendDataToPebble(dic, forceSend);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((PebbleBikeApplication)getApplication()).inject(this);
        _bus.register(this);
    }

    @Override
    public void onDestroy (){
        _bus.unregister(this);
        //_messageManager.hideWatchFace();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleIntent(Intent intent) {
        _messageThread = new Thread(_messageManager);
        _messageThread.start();

        _bus.post(new PebbleStatus(PebbleStatus.State.STARTED));
    }

    private void sendDataToPebble(PebbleDictionary data) {
        _messageManager.offer(data);
    }

    private void sendDataToPebbleIfPossible(PebbleDictionary data) {
        _messageManager.offerIfLow(data, 5);
    }
    private void sendDataToPebble(PebbleDictionary data, boolean forceSend) {
        //TODO(jay)
        //if (MainActivity.canvas_mode.equals("canvas_only")) {
        //    return;
        //}
        if (forceSend) {
            sendDataToPebble(data);
        } else {
            sendDataToPebbleIfPossible(data);
        }
    }
}