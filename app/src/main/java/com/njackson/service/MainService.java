package com.njackson.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.njackson.application.PebbleBikeApplication;
import com.njackson.gps.IForegroundServiceStarter;

import javax.inject.Inject;

/**
 * Created by njackson on 24/01/15.
 */
public class MainService extends Service {

    @Inject
    IForegroundServiceStarter _serviceStarter;
    private String TAG = "MainService";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        ((PebbleBikeApplication)getApplication()).inject(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Started Main Service");

        handleCommand(intent);
        _serviceStarter.startServiceForeground(this, "Pebble Bike", "GPS started");

        // ensures that if the service is recycled then it is restarted with the same refresh interval
        // onStartCommand will always be called with a non-null intent
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy () {
        _serviceStarter.stopServiceForeground(this);
        super.onDestroy();
    }

    private void handleCommand(Intent intent) {

    }
}
