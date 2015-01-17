package com.njackson.live;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;

import com.njackson.application.PebbleBikeApplication;
import com.njackson.events.GPSService.NewLocation;
import com.njackson.events.PebbleService.CurrentState;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;


public class LiveService extends Service {

    @Inject Bus _bus;
    @Inject SharedPreferences _sharedPreferences;

    private final String TAG = "PB-LiveService";

    private LiveTracking _liveTrackingJayps;
    private LiveTracking _liveTrackingMmt;
    Location firstLocation = null;

    @Subscribe
    public void onNewLocationEvent(NewLocation newLocation) {
        if (_sharedPreferences.getBoolean("LIVE_TRACKING", false)) {
            Location location = new Location("PebbleBike");
            location.setAccuracy(newLocation.getAccuracy());
            location.setLatitude(newLocation.getLatitude());
            location.setLongitude(newLocation.getLongitude());
            location.setTime(newLocation.getTime());

            if (location.getTime() > 0) {
                if (firstLocation == null) {
                    firstLocation = location;
                }

                _liveTrackingJayps.addPoint(firstLocation, location, location.getAltitude(), 0);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Started Live Service");
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
    public void onDestroy() {
        Log.i(TAG, "Destroy Live Service");
        _bus.unregister(this);
        super.onDestroy();
    }

    private void handleIntent(Intent intent) {
        _liveTrackingJayps = new LiveTracking(getApplicationContext(), LiveTracking.TYPE_JAYPS, _bus);
        _liveTrackingMmt = new LiveTracking(getApplicationContext(), LiveTracking.TYPE_MMT, _bus);

        _liveTrackingJayps.setLogin(_sharedPreferences.getString("LIVE_TRACKING_LOGIN", ""));
        _liveTrackingJayps.setPassword(_sharedPreferences.getString("LIVE_TRACKING_PASSWORD", ""));
        _liveTrackingJayps.setUrl(_sharedPreferences.getString("LIVE_TRACKING_URL", ""));

        _liveTrackingMmt.setLogin(_sharedPreferences.getString("LIVE_TRACKING_MMT_LOGIN", ""));
        _liveTrackingMmt.setPassword(_sharedPreferences.getString("LIVE_TRACKING_MMT_PASSWORD", ""));
        _liveTrackingMmt.setUrl(_sharedPreferences.getString("LIVE_TRACKING_MMT_URL", ""));

        _bus.post(new CurrentState(CurrentState.State.STARTED));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}