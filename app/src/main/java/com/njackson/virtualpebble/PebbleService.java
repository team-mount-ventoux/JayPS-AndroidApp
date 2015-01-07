package com.njackson.virtualpebble;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.njackson.Constants;
import com.njackson.application.PebbleBikeApplication;
import com.njackson.events.GPSService.NewLocation;
import com.njackson.events.PebbleService.CurrentState;
import com.njackson.utils.LocationEventConverter;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

import static com.njackson.events.GPSService.CurrentState.State.STARTED;


public class PebbleService extends Service {

    @Inject IMessageManager _messageManager;
    @Inject Bus _bus;

    private static final String TAG = "PB-PebbleService";
    private Thread _messageThread;

    @Subscribe
    public void onNewLocationEvent(NewLocation newLocation) {
        PebbleDictionary dictionary = LocationEventConverter.convert(
                newLocation,
                true /* serviceRunning */, // TODO(nic)
                false /* debug */,
                false/* liveTrackingEnabled */, // TODO(nic)
                1000 /* refreshInterval */, // TODO(nic)
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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}