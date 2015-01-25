package com.njackson.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.njackson.application.PebbleBikeApplication;
import com.njackson.events.MainService.MainServiceStatus;
import com.njackson.events.base.BaseStatus;
import com.njackson.gps.IForegroundServiceStarter;
import com.njackson.utils.time.ITimer;
import com.njackson.utils.time.ITimerHandler;
import com.squareup.otto.Bus;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by njackson on 24/01/15.
 */
public class MainService extends Service implements ITimerHandler {

    private String TAG = "MainService";

    @Inject IForegroundServiceStarter _serviceStarter;
    @Inject List<IServiceCommand> _serviceCommands;
    @Inject ITimer _timer;
    @Inject Bus _bus;

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
        Log.d(TAG,"Destroy");
        _serviceStarter.stopServiceForeground(this);

        disposeCommands();

        super.onDestroy();
    }

    private void handleCommand(Intent intent) {
        for(IServiceCommand command: _serviceCommands) {
            command.execute((PebbleBikeApplication)getApplication());
        }

        setupAutoStop();

        _bus.post(new MainServiceStatus(BaseStatus.Status.STARTED));
    }

    private void disposeCommands() {
        for(IServiceCommand command: _serviceCommands) {
            command.dispose();
        }
    }

    /*
        If none of the commands are STARTED then this service will auto terminate
     */
    private void setupAutoStop() {
        _timer.setRepeatingTimer(1000,this);
    }

    @Override
    public void handleTimeout() {
        boolean shouldContinue = false;

        for(IServiceCommand command: _serviceCommands) {
            if(command.getStatus() == BaseStatus.Status.STARTED) {
                shouldContinue = true;
            }
        }

        if(!shouldContinue) {
            _timer.cancel();
            stop();
        }
    }

    // Activity manager is not invoked with tests we need to wrap stop self to test it
    // has been called
    private void stop() {
        Log.d(TAG,"STOP ALL");
        _bus.post(new MainServiceStatus(BaseStatus.Status.STOPPED));
        this.stopSelf();
    }
}
