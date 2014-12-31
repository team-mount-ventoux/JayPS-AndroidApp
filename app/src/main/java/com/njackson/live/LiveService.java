package com.njackson.live;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import com.njackson.application.modules.PebbleBikeApplication;
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
    NewLocation firstLocation = null;

    @Subscribe
    public void onNewLocationEvent(NewLocation newLocation) {
        if (_sharedPreferences.getBoolean("LIVE_TRACKING", false)) {
            Log.i(TAG, "onNewLocationEvent time=" + newLocation.getTime());
            if (firstLocation == null) {
                firstLocation = newLocation;
            }
            _liveTrackingJayps.addPoint(firstLocation, newLocation, newLocation.getAltitude(), 0);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        handleIntent(intent);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        super.onCreate();
        ((PebbleBikeApplication)getApplication()).inject(this);
        _bus.register(this);
    }

    @Override
    public void onDestroy() {
        _bus.unregister(this);
        super.onDestroy();
    }

    private void handleIntent(Intent intent) {
        Log.i(TAG, "handleIntent");

        _liveTrackingJayps = new LiveTracking(getApplicationContext(), LiveTracking.TYPE_JAYPS);
        _liveTrackingMmt = new LiveTracking(getApplicationContext(), LiveTracking.TYPE_MMT);

        _liveTrackingJayps.setLogin(_sharedPreferences.getString("LIVE_TRACKING_LOGIN", ""));
        _liveTrackingJayps.setPassword(_sharedPreferences.getString("LIVE_TRACKING_PASSWORD", ""));
        _liveTrackingJayps.setUrl(_sharedPreferences.getString("LIVE_TRACKING_URL", ""));
        Log.d(TAG, "login=" + _sharedPreferences.getString("LIVE_TRACKING_LOGIN", ""));

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