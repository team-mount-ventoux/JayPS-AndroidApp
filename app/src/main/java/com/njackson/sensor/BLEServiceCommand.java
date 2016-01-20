package com.njackson.sensor;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import com.njackson.application.IInjectionContainer;
import com.njackson.application.modules.ForApplication;
import com.njackson.events.GPSServiceCommand.GPSStatus;
import com.njackson.events.BleServiceCommand.BleStatus;
import com.njackson.events.base.BaseStatus;
import com.njackson.service.IServiceCommand;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;


public class BLEServiceCommand implements IServiceCommand {

    private final String TAG = "PB-BleServiceCommand";

    @Inject @ForApplication Context _applicationContext;
    @Inject Bus _bus;
    @Inject SharedPreferences _sharedPreferences;
    @Inject
    IBle _hrm;
    IInjectionContainer _container;
    private BaseStatus.Status _currentStatus= BaseStatus.Status.NOT_INITIALIZED;
    private boolean _registrered_bus = false;

    @Override
    public void execute(IInjectionContainer container) {
        container.inject(this);
        _registrered_bus = false;
        if (isHrmActivated()) {
            _bus.register(this);
            _registrered_bus = true;
            _container = container;
            _currentStatus = BaseStatus.Status.INITIALIZED;
        }
    }

    @Override
    public void dispose() {
        if (isHrmActivated() && _registrered_bus) {
            _bus.unregister(this);
            _registrered_bus = false;
        }
    }

    private boolean isHrmActivated() {
        return _applicationContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
                // note: double check FEATURE_BLUETOOTH_LE + android version because the 1st test (hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) seems to return true on some 4.1 & 4.2
                && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2 // BLE requires 4.3 (Api level 18)
                && (!_sharedPreferences.getString("hrm_address", "").equals("") || !_sharedPreferences.getString("hrm_address2", "").equals("") || !_sharedPreferences.getString("hrm_address3", "").equals(""));
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

        if (!_sharedPreferences.getString("hrm_address", "").equals("") || !_sharedPreferences.getString("hrm_address2", "").equals("") || !_sharedPreferences.getString("hrm_address3", "").equals("")) {
            _hrm.start(_sharedPreferences.getString("hrm_address", ""), _sharedPreferences.getString("hrm_address2", ""), _sharedPreferences.getString("hrm_address3", ""), _bus, _container);
            _currentStatus = BaseStatus.Status.STARTED;
        } else {
            _currentStatus = BaseStatus.Status.UNABLE_TO_START;
        }
        _bus.post(new BleStatus(_currentStatus));
    }

    public void stop() {
        Log.d(TAG, "stop");
        if(_currentStatus != BaseStatus.Status.STARTED) {
            Log.d(TAG, "not started, unable to stop");
            return;
        }
        _hrm.stop();
        _currentStatus = BaseStatus.Status.STOPPED;
        _bus.post(new BleStatus(_currentStatus));
    }
}