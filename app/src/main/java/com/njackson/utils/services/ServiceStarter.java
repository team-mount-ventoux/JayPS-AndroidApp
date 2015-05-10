package com.njackson.utils.services;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.njackson.Constants;
import com.njackson.events.ActivityRecognitionCommand.ActivityRecognitionChangeState;
import com.njackson.events.GPSServiceCommand.GPSChangeState;
import com.njackson.events.GPSServiceCommand.GPSStatus;
import com.njackson.events.GoogleFitCommand.GoogleFitChangeState;
import com.njackson.events.LiveServiceCommand.LiveChangeState;
import com.njackson.events.base.BaseChangeState;
import com.njackson.events.base.BaseStatus;
import com.njackson.events.rx.MainServiceStatusObservable;
import com.njackson.service.MainService;
import com.squareup.otto.Bus;

import rx.Subscription;
import rx.functions.Action1;

/**
 * Created by njackson on 03/01/15.
 */
public class ServiceStarter implements IServiceStarter {

    private String TAG = "PB-ServiceStarter";

    private final Bus _bus;
    Context _context;
    SharedPreferences _sharedPreferences;
    Subscription _deferred;

    public ServiceStarter(Context context, SharedPreferences preferences, Bus bus) {
        _context = context;
        _sharedPreferences = preferences;
        _bus = bus;
    }

    @Override
    public void startActivityService() {
        startMainServiceIfNotRunning(new Action1<BaseStatus.Status>() {
            @Override
            public void call(BaseStatus.Status status) {
                _bus.post(new ActivityRecognitionChangeState(BaseChangeState.State.START));
            }
        });
    }

    @Override
    public void stopActivityService() {
        _bus.post(new ActivityRecognitionChangeState(BaseChangeState.State.STOP));
    }

    @Override
    public boolean isLocationServicesRunning() {
        return serviceRunning(MainService.class);
    }
    @Override
    public void startLocationServices() {
        startMainServiceIfNotRunning(new Action1<BaseStatus.Status>() {
            @Override
            public void call(BaseStatus.Status status) {
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
        if (serviceRunning(MainService.class)) {
            //Log.d(TAG, "broadcastLocationState/main service running, broadcast ANNOUNCE_STATE");
            _bus.post(new GPSChangeState(BaseChangeState.State.ANNOUNCE_STATE));
        } else {
            //Log.d(TAG, "broadcastLocationState/main service not running, broadcast STOPPED");
            _bus.post(new GPSStatus(BaseStatus.Status.STOPPED));
        }
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
        /* Disable Google Fit for v2 (shifted to v2.1+)
        boolean fit_start = _sharedPreferences.getBoolean("GOOGLE_FIT",false);
        if(fit_start) {
            startGoogleFitService();
        }
        */
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

    protected void startMainServiceIfNotRunning(final Action1<BaseStatus.Status> action) {
        if(!serviceRunning(MainService.class)) {
            _deferred = rx.Observable.create(new MainServiceStatusObservable(_bus))
                    .first()
                    .subscribe(action);
            startMainService();
        } else {
            action.call(BaseStatus.Status.STARTED);
        }
    }

    protected void startMainService() {
        _context.startService(new Intent(_context, MainService.class));
    }

    protected void stopMainService() {
        _context.stopService(new Intent(_context, MainService.class));
    }

    protected void startGPSService() {
        int refreshInterval = Integer.valueOf(_sharedPreferences.getString("REFRESH_INTERVAL", String.valueOf(Constants.REFRESH_INTERVAL_DEFAULT)));

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
