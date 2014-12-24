package com.njackson.virtualpebble;

import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.IBinder;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.njackson.application.modules.PebbleBikeApplication;
import com.njackson.events.GPSService.NewLocation;
import com.njackson.events.PebbleService.CurrentState;
import com.njackson.utils.LocationEventConverter;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;


public class PebbleService extends Service {

    @Inject IMessageManager _messageManager;
    @Inject Bus _bus;

    private final String TAG = "PB-VirtualPebble";
    private Thread _messageThread;

    @Subscribe
    public void onNewLocationEvent(NewLocation newLocation) {
        PebbleDictionary dictionary = LocationEventConverter.convert(newLocation, false, false, false, 1000, 0);
        sendDataToPebble(dictionary);
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
        super.onDestroy();
    }

    private void handleIntent(Intent intent) {
        _messageManager.setContext(getApplicationContext());
        _messageThread = new Thread(_messageManager);
        _messageThread.start();

        _bus.post(new CurrentState(CurrentState.State.STARTED));
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