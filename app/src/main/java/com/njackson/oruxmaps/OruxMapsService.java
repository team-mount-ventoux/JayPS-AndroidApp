package com.njackson.oruxmaps;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import com.njackson.application.PebbleBikeApplication;
import com.njackson.utils.time.ITime;
import com.squareup.otto.Bus;

import javax.inject.Inject;

/**
 * Created by njackson on 17/01/15.
 */
public class OruxMapsService extends Service {

    private static final String TAG = "OruxMapsService";

    @Inject IOruxMaps _oruxMaps;
    @Inject Bus _bus;
    @Inject SharedPreferences _sharedPreferences;
    @Inject ITime _time;

    private final long TWELVE_HOURS_MS = 12 * 3600 * 1000;

    @Override
    public void onCreate() {
        super.onCreate();

        ((PebbleBikeApplication)getApplication()).inject(this);
        _bus.register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Started GPS Service");

        handleCommand(intent);

        // ensures that if the service is recycled then it is restarted with the same refresh interval
        // onStartCommand will always be called with a non-null intent
        return START_REDELIVER_INTENT;
    }

    private void handleCommand(Intent intent) {
        String oruxmaps_auto = _sharedPreferences.getString("ORUXMAPS_AUTO", "disable");
        if (oruxmaps_auto.equals("continue")) {
            _oruxMaps.startRecordContinue();
        } else if (oruxmaps_auto.equals("new_segment")) {
            _oruxMaps.startRecordNewSegment();
        } else if (oruxmaps_auto.equals("new_track")) {
            _oruxMaps.startRecordNewTrack();
        } else if (oruxmaps_auto.equals("auto")) {
            if (lastStartGreaterThan12Hours()) { // 12 hours
                _oruxMaps.startRecordNewTrack();
            } else {
                _oruxMaps.startRecordNewSegment();
            }
        }
    }

    private boolean lastStartGreaterThan12Hours() {
        long last_start = _sharedPreferences.getLong("GPS_LAST_START", 0);
        return (_time.getCurrentTimeMilliseconds() - last_start) > TWELVE_HOURS_MS;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (!_sharedPreferences.getString("ORUXMAPS_AUTO", "disable").equals("disable")) {
            _oruxMaps.stopRecord();
        }
        super.onDestroy();
    }

}
