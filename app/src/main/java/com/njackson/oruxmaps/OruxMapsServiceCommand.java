package com.njackson.oruxmaps;

import android.content.SharedPreferences;
import com.njackson.application.IInjectionContainer;
import com.njackson.events.GPSServiceCommand.GPSStatus;
import com.njackson.events.base.BaseStatus;
import com.njackson.service.IServiceCommand;
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
    @Inject ITime _time;

    private final long TWELVE_HOURS_MS = 12 * 3600 * 1000;

    @Override
    public void execute(IInjectionContainer container) {
        container.inject(this);
        _bus.register(this);
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
    }

    public void stop() {
        if (!_sharedPreferences.getString("ORUXMAPS_AUTO", "disable").equals("disable")) {
            _oruxMaps.stopRecord();
        }
    }

    private boolean lastStartGreaterThan12Hours() {
        long last_start = _sharedPreferences.getLong("GPS_LAST_START", 0);
        return (_time.getCurrentTimeMilliseconds() - last_start) > TWELVE_HOURS_MS;
    }
}
