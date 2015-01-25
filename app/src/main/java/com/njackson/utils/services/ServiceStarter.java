package com.njackson.utils.services;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.njackson.activityrecognition.ActivityRecognitionServiceCommand;
import com.njackson.events.ActivityRecognitionCommand.ActivityRecognitionChangeState;
import com.njackson.events.GPSServiceCommand.GPSChangeState;
import com.njackson.events.GoogleFitCommand.GoogleFitChangeState;
import com.njackson.events.LiveServiceCommand.LiveChangeState;
import com.njackson.events.base.BaseChangeState;
import com.njackson.fit.GoogleFitServiceCommand;
import com.njackson.gps.GPSServiceCommand;
import com.njackson.live.LiveServiceCommand;
import com.njackson.oruxmaps.OruxMapsServiceCommand;
import com.njackson.pebble.PebbleServiceCommand;
import com.njackson.service.MainService;
import com.squareup.otto.Bus;

/**
 * Created by njackson on 03/01/15.
 */
public class ServiceStarter implements IServiceStarter {

    private final Bus _bus;
    Context _context;
    SharedPreferences _sharedPreferences;

    public ServiceStarter(Context context, SharedPreferences preferences, Bus bus) {
        _context = context;
        _sharedPreferences = preferences;
        _bus = bus;
    }

    @Override
    public void startActivityService() {
        startMainServiceIfNotRunning();
        _bus.post(new ActivityRecognitionChangeState(BaseChangeState.State.START));
    }

    @Override
    public void stopActivityService() {
        _bus.post(new ActivityRecognitionChangeState(BaseChangeState.State.STOP));
    }

    @Override
    public void startGoogleFitService() {
        startMainServiceIfNotRunning();
        _bus.post(new GoogleFitChangeState(BaseChangeState.State.START));
    }

    @Override
    public void stopGoogleFitService() {
        _bus.post(new GoogleFitChangeState(BaseChangeState.State.STOP));
    }

    @Override
    public void startLocationServices() {
        startMainServiceIfNotRunning();
        startGPSService();
        startLiveService();
        startActivityServiceIfEnabled();
        startGoogleFitServiceIfEnabled();
    }

    @Override
    public void stopLocationServices() {
        stopGPSService();
        stopLiveService();
        stopGoogleFitService();
        stopActivityServiceIfNotSticky();
    }

    @Override
    public void broadcastLocationState() {
        _bus.post(new GPSChangeState(BaseChangeState.State.ANNOUNCE_STATE));
    }

    @Override
    public boolean serviceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) _context.getSystemService(_context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startGoogleFitServiceIfEnabled() {
        boolean fit_start = _sharedPreferences.getBoolean("GOOGLE_FIT",false);
        if(fit_start) {
            startGoogleFitService();
        }
    }

    private void startActivityServiceIfEnabled() {
        boolean activity_start = _sharedPreferences.getBoolean("ACTIVITY_RECOGNITION",false);
        boolean fit_start = _sharedPreferences.getBoolean("GOOGLE_FIT",false);
        if(activity_start || fit_start) {
            startActivityService();
        }
    }

    private void stopActivityServiceIfNotSticky() {
        boolean activity_start = _sharedPreferences.getBoolean("ACTIVITY_RECOGNITION",false);
        if(!activity_start) {
            stopActivityService();
        }
    }

    protected void startMainServiceIfNotRunning() {
        if(!serviceRunning(MainService.class)) {
            startMainService();
        }
    }

    protected void startMainService() {
        _context.startService(new Intent(_context, MainService.class));
    }

    protected void stopMainService() {
        _context.stopService(new Intent(_context, MainService.class));
    }

    protected void startGPSService() {
        int refreshInterval = Integer.valueOf(_sharedPreferences.getString("REFRESH_INTERVAL", "1000"));

        _bus.post(new GPSChangeState(BaseChangeState.State.START, refreshInterval));
    }

    private void stopGPSService() {
        _bus.post(new GPSChangeState(BaseChangeState.State.STOP));
    }

    private void startLiveService() {
        _bus.post(new LiveChangeState(BaseChangeState.State.START));
    }

    private void stopLiveService() {
        _bus.post(new LiveChangeState(BaseChangeState.State.STOP));
    }
}
