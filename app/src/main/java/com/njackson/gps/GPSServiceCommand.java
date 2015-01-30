package com.njackson.gps;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;

import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.os.Bundle;
import android.util.Log;

import com.njackson.Constants;
import com.njackson.adapters.AdvancedLocationToNewLocation;
import com.njackson.application.IInjectionContainer;
import com.njackson.application.modules.ForApplication;
import com.njackson.events.GPSServiceCommand.ChangeRefreshInterval;
import com.njackson.events.GPSServiceCommand.GPSChangeState;
import com.njackson.events.GPSServiceCommand.GPSStatus;
import com.njackson.events.GPSServiceCommand.ResetGPSState;
import com.njackson.events.GPSServiceCommand.NewLocation;
import com.njackson.events.HrmServiceCommand.HrmHeartRate;
import com.njackson.events.base.BaseStatus;
import com.njackson.service.IServiceCommand;
import com.njackson.state.IGPSDataStore;
import com.njackson.utils.time.ITime;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.concurrent.Callable;

import javax.inject.Inject;

import fr.jayps.android.AdvancedLocation;

/**
 * Created with IntelliJ IDEA.
 * User: njackson
 * Date: 23/05/2013
 * Time: 13:30
 * To change this template use File | Settings | File Templates.
 */
public class GPSServiceCommand implements IServiceCommand {

    private static final String TAG = "PB-GPSServiceCommand";

    @Inject @ForApplication
    Context _applicationContext;

    @Inject LocationManager _locationMgr;
    @Inject SensorManager _sensorManager;
    @Inject IGPSDataStore _dataStore;
    @Inject Bus _bus;
    @Inject ITime _time;

    private AdvancedLocation _advancedLocation;
    private Location firstLocation = null;
    private ServiceNmeaListener _nmeaListener;
    private GPSSensorEventListener _sensorListener;
	private int _heartRate = 0;
    private BaseStatus.Status _currentStatus= BaseStatus.Status.NOT_INITIALIZED;

    @Subscribe
    public void onResetGPSStateEvent(ResetGPSState event) {
        //stop service stopLocationUpdates();
        resetGPSStats();
    }

    @Subscribe
    public void onGPSRefreshChangeEvent(ChangeRefreshInterval event) {
        changeRefreshInterval(event.getRefreshInterval());
    }

    @Subscribe
    public void onGPSChangeState(GPSChangeState event) {
        switch(event.getState()) {
            case START:
                if(_currentStatus != BaseStatus.Status.STARTED)
                    start(event.getRefreshInterval());
                break;
            case STOP:
                if(_currentStatus != BaseStatus.Status.STOPPED)
                    stop();
                break;
            case ANNOUNCE_STATE:
                broadcastStatus(_currentStatus);
        }
    }

    @Subscribe
    public void onNewHeartRate(HrmHeartRate event) {
        Log.d(TAG, "onNewHeartRate:" + event.getHeartRate());
        _heartRate = event.getHeartRate();
    }

    @Override
    public void execute(IInjectionContainer container) {
        container.inject(this);
        _bus.register(this);

        _currentStatus = BaseStatus.Status.INITIALIZED;
        _advancedLocation = new AdvancedLocation();
    }

    @Override
    public void dispose() {
        _bus.unregister(this);
    }

    @Override
    public BaseStatus.Status getStatus() {
        return _currentStatus;
    }

    private void start(int refreshInterval) {
        Log.d(TAG, "Start");

        createNewAdvancedLocation();
        loadGPSStats();

        // check to see if GPS is enabled
        if(checkGPSEnabled(_locationMgr)) {
            requestLocationUpdates(refreshInterval);
            registerNmeaListener();
            registerSensorListener();
            setGPSStartTime();

            _currentStatus = BaseStatus.Status.STARTED;
            broadcastStatus(_currentStatus);

            // send the saved values directly to update the watch
            // TODO(jay) send xpos=0, ypos=0, it will display a "wrong" point on the map
            broadcastLocation(0, 0);
        } else {
            _currentStatus = BaseStatus.Status.DISABLED;
            broadcastStatus(_currentStatus);
        }
    }

    public void stop (){
        Log.d(TAG, "Destroy GPS Service");
        saveGPSStats();

        stopLocationUpdates();

        _currentStatus = BaseStatus.Status.STOPPED;
        broadcastStatus(_currentStatus);
    }

    private void setGPSStartTime() {
        _dataStore.setStartTime(_time.getCurrentTimeMilliseconds());
        _dataStore.commit();
    }

    private boolean checkGPSEnabled(LocationManager locationMgr) {
        return locationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    // load the saved state
    private void loadGPSStats() {
        Log.d(TAG, "loadGPSStats()");

        _advancedLocation.setDistance(_dataStore.getDistance());
        _advancedLocation.setElapsedTime(_dataStore.getElapsedTime());

        try {
            _advancedLocation.setAscent(_dataStore.getAscent());
        } catch (ClassCastException e) {
            _advancedLocation.setAscent(0.0);
        }

        _advancedLocation.setGeoidHeight(_dataStore.getGEOIDHeight());

        if (_dataStore.getFirstLocationLattitude() != 0.0f && _dataStore.getFirstLocationLongitude() != 0.0f) {
            firstLocation = new Location("PebbleBike");
            firstLocation.setLatitude(_dataStore.getFirstLocationLattitude());
            firstLocation.setLongitude(_dataStore.getFirstLocationLongitude());
        } else {
            firstLocation = null;
        }
    }

    // save the state
    private void saveGPSStats() {
        _dataStore.setDistance(_advancedLocation.getDistance());
        _dataStore.setElapsedTime(_advancedLocation.getElapsedTime());
        _dataStore.setAscent((float) _advancedLocation.getAscent());
        _dataStore.setGEOIDHeight((float) _advancedLocation.getGeoidHeight());
        if (firstLocation != null) {
            _dataStore.setFirstLocationLattitude((float) firstLocation.getLatitude());
            _dataStore.setFirstLocationLongitude((float) firstLocation.getLongitude());
        }
        _dataStore.commit();
    }

    // reset the saved state
    private void resetGPSStats() {
        _dataStore.resetAllValues();
        _dataStore.commit();

        // GPS is running
        // reninit all properties
        createNewAdvancedLocation();

        loadGPSStats();
    }

    private void createNewAdvancedLocation() {
        _advancedLocation = new AdvancedLocation(_applicationContext);
        _advancedLocation.debugLevel = 0; //debug ? 2 : 0;
        _advancedLocation.debugTagPrefix = "PB-";
    }

    private void requestLocationUpdates(long refresh_interval) {
        if (_currentStatus != BaseStatus.Status.STARTED) {
            _locationMgr.removeUpdates(_locationListener);
        }
        _locationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, refresh_interval, 2.0f, _locationListener);
    }

    private void registerNmeaListener() {
        _nmeaListener = new ServiceNmeaListener(_advancedLocation,_locationMgr, _dataStore);
        _locationMgr.addNmeaListener(_nmeaListener);
    }

    private void registerSensorListener() {
        _sensorListener = new GPSSensorEventListener(_advancedLocation,_sensorManager,new Callable() {
            @Override
            public Object call() throws Exception {
                return null;
            }
        });

        // delay between events in microseconds
        _sensorManager.registerListener(_sensorListener, _sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE), 3000000);
    }

    private void stopLocationUpdates() {
        _locationMgr.removeUpdates(_locationListener);
        _locationMgr.removeNmeaListener(_nmeaListener);
        _sensorManager.unregisterListener(_sensorListener);
    }

    private void changeRefreshInterval(int refreshInterval) {
        requestLocationUpdates(refreshInterval);
    }

    private LocationListener _locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            _advancedLocation.onLocationChanged(location);
            if (firstLocation == null) {
                firstLocation = location;
                saveGPSStats();
            }

            double xpos = firstLocation.distanceTo(location) * Math.sin(firstLocation.bearingTo(location)/180*3.1415);
            double ypos = firstLocation.distanceTo(location) * Math.cos(firstLocation.bearingTo(location)/180*3.1415);

            xpos = Math.floor(xpos/10);
            ypos = Math.floor(ypos/10);
            broadcastLocation(xpos, ypos);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }

    };

    private void broadcastLocation(double xpos, double ypos) {
        int units = _dataStore.getMeasurementUnits();

        NewLocation event = new AdvancedLocationToNewLocation(_advancedLocation, xpos, ypos, units);
        if (_heartRate > 0) {
            event.setHeartRate(_heartRate);
        }

        _bus.post(event);
    }

    private void broadcastStatus(BaseStatus.Status currentStatus) {
        _bus.post(new GPSStatus(currentStatus));
    }
}