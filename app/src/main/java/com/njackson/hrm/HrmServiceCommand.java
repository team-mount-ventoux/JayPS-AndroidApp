package com.njackson.hrm;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.njackson.application.IInjectionContainer;
import com.njackson.application.modules.ForApplication;
import com.njackson.events.GPSServiceCommand.GPSStatus;
import com.njackson.events.HrmServiceCommand.HrmStatus;
import com.njackson.events.base.BaseStatus;
import com.njackson.service.IServiceCommand;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;
import javax.inject.Named;


public class HrmServiceCommand implements IServiceCommand {

    private final String TAG = "PB-HrmServiceCommand";

    @Inject @ForApplication Context _applicationContext;
    @Inject Bus _bus;
    @Inject SharedPreferences _sharedPreferences;
    @Inject IHrm _hrm;
    IInjectionContainer _container;
    private BaseStatus.Status _currentStatus= BaseStatus.Status.NOT_INITIALIZED;

    @Override
    public void execute(IInjectionContainer container) {
        container.inject(this);
        _bus.register(this);
        _container = container;
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
                if(_currentStatus != BaseStatus.Status.STARTED) {
                    start();
                }
                break;
            case STOPPED:
                if(_currentStatus == BaseStatus.Status.STARTED) {
                    stop();
                }
        }
    }

    private void start() {
        Log.d(TAG, "start");

        if (!_sharedPreferences.getString("hrm_address", "").equals("")) {
            _hrm.start(_sharedPreferences.getString("hrm_address", ""), _bus, _container);
            _currentStatus = BaseStatus.Status.STARTED;
        } else {
            _currentStatus = BaseStatus.Status.UNABLE_TO_START;
        }
        _bus.post(new HrmStatus(_currentStatus));
    }

    public void stop() {
        Log.d(TAG, "stop");
        if(_currentStatus != BaseStatus.Status.STARTED) {
            Log.d(TAG, "not started, unable to stop");
            return;
        }
        _hrm.stop();
        _currentStatus = BaseStatus.Status.STOPPED;
        _bus.post(new HrmStatus(_currentStatus));
    }
}