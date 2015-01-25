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
import com.njackson.events.MainService.MainServiceStatus;
import com.njackson.events.base.BaseChangeState;
import com.njackson.fit.GoogleFitServiceCommand;
import com.njackson.gps.GPSServiceCommand;
import com.njackson.live.LiveServiceCommand;
import com.njackson.oruxmaps.OruxMapsServiceCommand;
import com.njackson.pebble.PebbleServiceCommand;
import com.njackson.service.MainService;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.jdeferred.DeferredManager;
import org.jdeferred.DoneCallback;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by njackson on 03/01/15.
 */
public class ServiceStarter implements IServiceStarter {

    private final Bus _bus;
    Context _context;
    SharedPreferences _sharedPreferences;
    Promise _promise;
    DeferredObject _deferred;

    public ServiceStarter(Context context, SharedPreferences preferences, Bus bus) {
        _context = context;
        _sharedPreferences = preferences;
        _bus = bus;
    }

    @Subscribe
    public void onMainServiceStatus(MainServiceStatus event) {
        _bus.unregister(this);
        if(_deferred != null && !_deferred.isResolved()) {
            _deferred.resolve(event);
        }
    }

    @Override
    public void startActivityService() {
        startMainServiceIfNotRunning(new DoneCallback() {
            @Override
            public void onDone(Object result) {
                _bus.post(new ActivityRecognitionChangeState(BaseChangeState.State.START));
            }
        });
    }

    @Override
    public void stopActivityService() {
        _bus.post(new ActivityRecognitionChangeState(BaseChangeState.State.STOP));
    }

    @Override
    public void startLocationServices() {
        startMainServiceIfNotRunning(new DoneCallback() {
            @Override
            public void onDone(Object result) {
                startGPSService();
                startLiveService();
                startActivityServiceIfEnabled();
                startGoogleFitServiceIfEnabled();
            }
        });
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

    protected void startMainServiceIfNotRunning(DoneCallback callback) {
        _deferred = new DeferredObject();
        _deferred.then(callback);

        if(!serviceRunning(MainService.class)) {
            startMainService();
        } else {
            _deferred.resolve("Service Running");
        }
    }

    protected void startMainService() {
        _bus.register(this);
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

    private void startGoogleFitService() {
        _bus.post(new GoogleFitChangeState(BaseChangeState.State.START));
    }

    private void stopGoogleFitService() {
        _bus.post(new GoogleFitChangeState(BaseChangeState.State.STOP));
    }
}
