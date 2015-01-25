package com.njackson.hrm;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.njackson.application.IInjectionContainer;
import com.njackson.application.modules.ForApplication;
import com.njackson.events.GPSServiceCommand.GPSStatus;
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

    @Override
    public void execute(IInjectionContainer container) {
        container.inject(this);
        _bus.register(this);
    }

    @Subscribe
    public void onGPSStatusEvent(GPSStatus event) {
        switch(event.getState()) {
            case STARTED:
                start();
                break;
            case STOPPED:
                stop();
        }
    }

    private void start() {
        Log.d(TAG, "start");
        _hrm.start(_sharedPreferences.getString("hrm_address", ""), _bus);
    }

    public void stop() {
        Log.d(TAG, "stop");
        _hrm.stop();
    }
}