package com.njackson.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.njackson.activityrecognition.ActivityRecognitionServiceCommand;
import com.njackson.application.PebbleBikeApplication;
import com.njackson.fit.GoogleFitServiceCommand;
import com.njackson.gps.GPSServiceCommand;
import com.njackson.gps.IForegroundServiceStarter;
import com.njackson.live.LiveServiceCommand;
import com.njackson.oruxmaps.OruxMapsServiceCommand;
import com.njackson.pebble.PebbleServiceCommand;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

/**
 * Created by njackson on 24/01/15.
 */
public class MainService extends Service {

    private String TAG = "MainService";

    @Inject IForegroundServiceStarter _serviceStarter;
    @Inject List<IServiceCommand> _serviceCommands;

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
        for(IServiceCommand command: _serviceCommands) {
            command.execute((PebbleBikeApplication)getApplication());
        }
    }
}
