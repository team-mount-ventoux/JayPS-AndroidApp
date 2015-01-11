package com.njackson.virtualpebble;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.njackson.Constants;
import com.njackson.application.PebbleBikeApplication;
import com.njackson.events.GPSService.NewLocation;
import com.njackson.events.LiveService.LiveMessage;
import com.njackson.events.PebbleService.CurrentState;
import com.njackson.utils.LocationEventConverter;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

import static com.njackson.events.GPSService.CurrentState.State.STARTED;


public class PebbleService extends Service {

    @Inject IMessageManager _messageManager;
    @Inject SharedPreferences _sharedPreferences;
    @Inject Bus _bus;

    private static final String TAG = "PB-PebbleService";
    private Thread _messageThread;

    private int _numberOfFriendsSentToPebble = 0;

    @Subscribe
    public void onNewLocationEvent(NewLocation newLocation) {
        PebbleDictionary dictionary = LocationEventConverter.convert(
                newLocation,
                true /* serviceRunning */, // TODO(nic)
                _sharedPreferences.getBoolean("PREF_DEBUG", false) /* debug */,
                _sharedPreferences.getBoolean("LIVE_TRACKING", false) /* liveTrackingEnabled */,
                Integer.valueOf(_sharedPreferences.getString("REFRESH_INTERVAL", "1000")) /* refreshInterval */,
                255 /* heartRate */
        );
        sendDataToPebble(dictionary);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return START_STICKY;
    }

    @Subscribe
    public void onGPSServiceState(com.njackson.events.GPSService.CurrentState event) {

        if (event.getState().compareTo(com.njackson.events.GPSService.CurrentState.State.STOPPED) == 0) {
            Log.d(TAG, "onGPSServiceState STOPPED");
            PebbleDictionary dictionary = new PebbleDictionary();
            dictionary.addInt32(Constants.STATE_CHANGED,Constants.STATE_STOP);
            //Log.d(TAG, " STATE_CHANGED: "   + dictionary.getInteger(Constants.STATE_CHANGED));
            sendDataToPebble(dictionary);
        }
    }

    @Subscribe public void onLiveMessage(LiveMessage msg) {
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
        Log.d(TAG, sending);

        sendDataToPebble(dic, forceSend);
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
        _messageManager.hideWatchFace();
        super.onDestroy();
    }

    private void handleIntent(Intent intent) {
        _messageThread = new Thread(_messageManager);
        _messageThread.start();

        _bus.post(new CurrentState(CurrentState.State.STARTED));
        _messageManager.showWatchFace();
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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}