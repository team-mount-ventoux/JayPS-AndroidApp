package com.njackson.live;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import com.njackson.adapters.NewLocationToAndroidLocation;
import com.njackson.application.IInjectionContainer;
import com.njackson.application.modules.ForApplication;
import com.njackson.events.GPSServiceCommand.NewLocation;
import com.njackson.events.LiveServiceCommand.LiveChangeState;
import com.njackson.events.base.BaseChangeState;
import com.njackson.events.base.BaseStatus;
import com.njackson.service.IServiceCommand;
import com.njackson.state.IGPSDataStore;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;
import javax.inject.Named;


public class LiveServiceCommand implements IServiceCommand {

    private final String TAG = "PB-LiveServiceCommand";

    @Inject @ForApplication Context _applicationContext;
    @Inject Bus _bus;
    @Inject SharedPreferences _sharedPreferences;
    @Inject IGPSDataStore _dataStore;
    @Inject @Named("LiveTrackingNextcloud") ILiveTracking _liveTrackingNextcloud;
    @Inject @Named("LiveTrackingMmt") ILiveTracking _liveTrackingMmt;

    Location firstLocation = null;
    private BaseStatus.Status _currentStatus = BaseStatus.Status.NOT_INITIALIZED;

    @Subscribe
    public void onChangeStateEvent(LiveChangeState event) {
        if(event.getState() == BaseChangeState.State.START) {
            start();
        }
    }

    @Subscribe
    public void onNewLocationEvent(NewLocation newLocation) {
        Location location = new NewLocationToAndroidLocation("JayPS", newLocation);
        Log.d(TAG, "new loc");
        if (location.getTime() > 0) {
            if (firstLocation == null) {
                if (_dataStore.getFirstLocationLattitude() != 0.0f && _dataStore.getFirstLocationLongitude() != 0.0f) {
                    firstLocation = new Location("JayPS");
                    firstLocation.setLatitude(_dataStore.getFirstLocationLattitude());
                    firstLocation.setLongitude(_dataStore.getFirstLocationLongitude());
                } else {
                    // todo(jay) bug?
                }
            }
            if (_sharedPreferences.getBoolean("LIVE_TRACKING", false)) {
                _liveTrackingNextcloud.addPoint(firstLocation, location, newLocation.getHeartRate(), newLocation.getCyclingCadence(), newLocation.getBatteryLevel());
            }
            if (_sharedPreferences.getBoolean("LIVE_TRACKING_MMT", false)) {
                _liveTrackingMmt.addPoint(firstLocation, location, newLocation.getHeartRate(), newLocation.getCyclingCadence(), newLocation.getBatteryLevel());
            }
        }
    }

    @Override
    public void execute(IInjectionContainer container) {
        container.inject(this);
        _bus.register(this);
        _currentStatus = BaseStatus.Status.INITIALIZED;
    }

    @Override
    public void dispose() {
        _bus.unregister(this);
    }

    @Override
    public BaseStatus.Status getStatus() {
        return null;
    }

    private void start() {
        _liveTrackingNextcloud.setBus(_bus);
        StringBuilder ncURL = new StringBuilder();
        ncURL.append(_sharedPreferences.getString("LIVE_TRACKING_URL", ""));
        ncURL.append("/index.php/apps/phonetrack/logPost/");
        ncURL.append(_sharedPreferences.getString("LIVE_TRACKING_TOKEN", ""));
        ncURL.append("/");
        ncURL.append(_sharedPreferences.getString("LIVE_TRACKING_DEVICE", ""));
        _liveTrackingNextcloud.setUrl(ncURL.toString());

        _liveTrackingMmt.setLogin(_sharedPreferences.getString("LIVE_TRACKING_MMT_LOGIN", ""));
        _liveTrackingMmt.setPassword(_sharedPreferences.getString("LIVE_TRACKING_MMT_PASSWORD", ""));
        _liveTrackingMmt.setUrl(_sharedPreferences.getString("LIVE_TRACKING_MMT_URL", ""));
    }
}
