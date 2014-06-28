package com.njackson.virtualpebble;

import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.IBinder;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.njackson.application.modules.PebbleBikeApplication;
import com.njackson.events.GPSService.NewLocation;
import com.njackson.utils.LocationEventConverter;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;


public class PebbleService extends Service {

    @Inject IMessageManager _messageManager;
    @Inject Bus _bus;

    private final String TAG = "PB-VirtualPebble";

    public static Application app;

    private BroadcastReceiver _broadcastReceiver;

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
    }

    private void handleIntent(Intent intent) {
        _messageManager.setContext(getApplicationContext());
        new Thread(_messageManager).start();
    }

    @Subscribe
    public void onNewLocationEvent(NewLocation newLocation) {
        PebbleDictionary dictionary = LocationEventConverter.convert(newLocation, false, false, false, 1000, 0);
        sendDataToPebble(dictionary);
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