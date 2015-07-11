package com.njackson.oruxmaps;

import android.content.SharedPreferences;
import android.util.Log;

import com.njackson.application.IInjectionContainer;
import com.njackson.events.GPSServiceCommand.GPSStatus;
import com.njackson.events.base.BaseStatus;
import com.njackson.service.IServiceCommand;
import com.njackson.state.IGPSDataStore;
import com.njackson.utils.time.ITime;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

/**
 * Created by njackson on 17/01/15.
 */
public class OruxMapsServiceCommand implements IServiceCommand {

    private static final String TAG = "OruxMapsService";

    @Inject IOruxMaps _oruxMaps;
    @Inject Bus _bus;
    @Inject SharedPreferences _sharedPreferences;
    @Inject IGPSDataStore _dataStore;
    @Inject ITime _time;

    private final long TWELVE_HOURS_MS = 12 * 3600 * 1000;

    private BaseStatus.Status _currentStatus = BaseStatus.Status.NOT_INITIALIZED;

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

    @Subscribe
    public void onGPSStatusEvent(GPSStatus event) {
        switch(event.getStatus()) {
            case STARTED:
                start();
                break;
            case STOPPED:
                stop();
        }
    }

    private void start() {
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
        _currentStatus = BaseStatus.Status.STARTED;
    }

    public void stop() {
        if (!_sharedPreferences.getString("ORUXMAPS_AUTO", "disable").equals("disable")) {
            _oruxMaps.stopRecord();
        }
        _currentStatus = BaseStatus.Status.STOPPED;
    }

    private boolean lastStartGreaterThan12Hours() {
        long last_start = _dataStore.getPrevStartTime();
        Log.d(TAG, "last_start=" + last_start + " now=" + _time.getCurrentTimeMilliseconds());
        return (_time.getCurrentTimeMilliseconds() - last_start) > TWELVE_HOURS_MS;
    }
}
