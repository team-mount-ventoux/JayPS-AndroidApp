package com.njackson.live;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

import com.njackson.adapters.NewLocationToAndroidLocation;
import com.njackson.application.IInjectionContainer;
import com.njackson.application.modules.ForApplication;
import com.njackson.events.GPSServiceCommand.NewLocation;
import com.njackson.events.LiveServiceCommand.LiveChangeState;
import com.njackson.events.base.BaseChangeState;
import com.njackson.service.IServiceCommand;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;
import javax.inject.Named;


public class LiveServiceCommand implements IServiceCommand {

    private final String TAG = "PB-LiveService";

    @Inject @ForApplication Context _applicationContext;
    @Inject Bus _bus;
    @Inject SharedPreferences _sharedPreferences;
    @Inject @Named("LiveTrackingJayPS") ILiveTracking _liveTrackingJayps;
    @Inject @Named("LiveTrackingMmt") ILiveTracking _liveTrackingMmt;

    Location firstLocation = null;

    @Subscribe
    public void onChangeStateEvent(LiveChangeState event) {
        if(event.getState().compareTo(BaseChangeState.State.START) == 0) {
            start();
        }
    }

    @Subscribe
    public void onNewLocationEvent(NewLocation newLocation) {
        Location location = new NewLocationToAndroidLocation("PebbleBike", newLocation);

        if (location.getTime() > 0) {
            if (firstLocation == null) {
                firstLocation = location;
            }
            if (_sharedPreferences.getBoolean("LIVE_TRACKING", false)) {
                _liveTrackingJayps.addPoint(firstLocation, location, location.getAltitude(), newLocation.getHeartRate());
            }
            if (_sharedPreferences.getBoolean("LIVE_TRACKING_MMT", false)) {
                _liveTrackingMmt.addPoint(firstLocation, location, location.getAltitude(), 0);
            }
        }
    }

    @Override
    public void execute(IInjectionContainer container) {
        container.inject(this);
        _bus.register(this);
    }

    private void start() {
        _liveTrackingJayps.setLogin(_sharedPreferences.getString("LIVE_TRACKING_LOGIN", ""));
        _liveTrackingJayps.setPassword(_sharedPreferences.getString("LIVE_TRACKING_PASSWORD", ""));
        _liveTrackingJayps.setUrl(_sharedPreferences.getString("LIVE_TRACKING_URL", ""));

        _liveTrackingMmt.setLogin(_sharedPreferences.getString("LIVE_TRACKING_MMT_LOGIN", ""));
        _liveTrackingMmt.setPassword(_sharedPreferences.getString("LIVE_TRACKING_MMT_PASSWORD", ""));
        _liveTrackingMmt.setUrl(_sharedPreferences.getString("LIVE_TRACKING_MMT_URL", ""));
    }
}