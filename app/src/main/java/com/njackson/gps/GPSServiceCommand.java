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

import com.njackson.adapters.AdvancedLocationToNewLocation;
import com.njackson.adapters.NewLocationToSavedLocation;
import com.njackson.application.IInjectionContainer;
import com.njackson.application.modules.ForApplication;
import com.njackson.events.BleServiceCommand.BleCadence;
import com.njackson.events.GPSServiceCommand.ChangeRefreshInterval;
import com.njackson.events.GPSServiceCommand.GPSChangeState;
import com.njackson.events.GPSServiceCommand.GPSStatus;
import com.njackson.events.GPSServiceCommand.NewAltitude;
import com.njackson.events.GPSServiceCommand.ResetGPSState;
import com.njackson.events.GPSServiceCommand.NewLocation;
import com.njackson.events.GPSServiceCommand.SavedLocation;
import com.njackson.events.BleServiceCommand.BleHeartRate;
import com.njackson.events.base.BaseStatus;
import com.njackson.service.IServiceCommand;
import com.njackson.state.IGPSDataStore;
import com.njackson.utils.AltitudeGraphReduce;
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
    @Inject SharedPreferences _sharedPreferences;
    @Inject AltitudeGraphReduce _altitudeGraphReduce;

    private AdvancedLocation _advancedLocation;
    private Location firstLocation = null;
    private ServiceNmeaListener _nmeaListener;
    private GPSSensorEventListener _sensorListener;
	private int _heartRate = 0;
    private int _cadence = 0;
    private BaseStatus.Status _currentStatus= BaseStatus.Status.NOT_INITIALIZED;
    private SavedLocation _savedLocation = null;
    private NewAltitude _savedNewAltitude = null;
    private int _nbLocationReceived = 0;

    private long _last_post_newlocation  = 0;
    private int _refresh_interval = 0;

    @Subscribe
    public void onResetGPSStateEvent(ResetGPSState event) {
        //stop service stopLocationUpdates();
        resetGPSStats();
    }

    @Subscribe
    public void onGPSRefreshChangeEvent(ChangeRefreshInterval event) {
        _refresh_interval = event.getRefreshInterval();
        changeRefreshInterval(event.getRefreshInterval());
    }

    @Subscribe
    public void onGPSChangeState(GPSChangeState event) {
        switch(event.getState()) {
            case START:
                if(_currentStatus != BaseStatus.Status.STARTED) {
                    _refresh_interval = event.getRefreshInterval();
                    start(event.getRefreshInterval());
                }
                broadcastStatus(_currentStatus);
                break;
            case STOP:
                if(_currentStatus != BaseStatus.Status.STOPPED) {
                    stop();
                }
                broadcastStatus(_currentStatus);
                break;
            case ANNOUNCE_STATE:
                broadcastStatus(_currentStatus);
        }
    }

    @Subscribe
    public void onNewHeartRate(BleHeartRate event) {
        Log.d(TAG, "onNewHeartRate:" + event.getHeartRate());
        _heartRate = event.getHeartRate();
        broadcastLocation(null);
    }

    @Subscribe
    public void onNewCadence(BleCadence event) {
        Log.d(TAG, "onNewCadence:" + event.getCadence());
        _cadence = event.getCadence();
        broadcastLocation(null);
    }

    @Override
    public void execute(IInjectionContainer container) {
        container.inject(this);
        _bus.register(this);

        _currentStatus = BaseStatus.Status.INITIALIZED;
        createNewAdvancedLocation();
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

            // send the saved values directly to update the watch
            // TODO(jay) send xpos=0, ypos=0, it will display a "wrong" point on the map
            broadcastLocation(null);
        } else {
            _currentStatus = BaseStatus.Status.DISABLED;
        }
    }

    public void stop (){
        saveGPSStats();

        stopLocationUpdates();

        _currentStatus = BaseStatus.Status.STOPPED;
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
        _advancedLocation.setDistance(_dataStore.getDistance());
        _advancedLocation.setElapsedTime(_dataStore.getElapsedTime());

        try {
            _advancedLocation.setAscent(_dataStore.getAscent());
        } catch (ClassCastException e) {
            _advancedLocation.setAscent(0.0);
        }

        _advancedLocation.setNbAscent(_dataStore.getNbAscent());
        _advancedLocation.setMaxSpeed(_dataStore.getMaxSpeed());

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
        _dataStore.setNbAscent(_advancedLocation.getNbAscent());
        _dataStore.setMaxSpeed(_advancedLocation.getMaxSpeed());
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

        _altitudeGraphReduce.resetData();
    }

    private void createNewAdvancedLocation() {
        _advancedLocation = new AdvancedLocation(_applicationContext);
        _advancedLocation.debugLevel = _sharedPreferences.getBoolean("PREF_DEBUG", false) ? 1 : 0;
        _advancedLocation.debugTagPrefix = "PB-";
        _advancedLocation.setSaveLocation(_sharedPreferences.getBoolean("ENABLE_TRACKS", false));
    }

    private void requestLocationUpdates(long refresh_interval) {
        if (_currentStatus == BaseStatus.Status.STARTED) {
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
                //Log.d(TAG, "call:" + _advancedLocation.getAltitudeFromPressure());
                broadcastLocation(null);
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
            _advancedLocation.onLocationChanged(location, _heartRate, _cadence);
            if (firstLocation == null) {
                firstLocation = location;
                saveGPSStats();
            }
            _nbLocationReceived++;
            if (_nbLocationReceived % 100 == 0) {
                // save stats every 100 new locations
                saveGPSStats();
            }

            broadcastLocation(location);
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

    private double _xpos = 0;
    private double _ypos = 0;
    private void broadcastLocation(Location location) {
        if (firstLocation != null && location != null) {
            _xpos = firstLocation.distanceTo(location) * Math.sin(firstLocation.bearingTo(location) / 180 * 3.1415);
            _xpos = Math.floor(_xpos / 10);
            _ypos = firstLocation.distanceTo(location) * Math.cos(firstLocation.bearingTo(location) / 180 * 3.1415);
            _ypos = Math.floor(_ypos / 10);
        }
        int units = _dataStore.getMeasurementUnits();

        NewLocation event = new AdvancedLocationToNewLocation(_advancedLocation, _xpos, _ypos, units);
        if (_heartRate > 0) {
            event.setHeartRate(_heartRate);
        }
        if (_cadence > 0) {
            event.setCadence(_cadence);
        }

        _savedLocation = new NewLocationToSavedLocation(event);

        if (_time.getCurrentTimeMilliseconds() - _last_post_newlocation > _refresh_interval * 0.95) {
            // 0.95 to avoid skipping wanted data
            //Log.d(TAG, "ts:" + _time.getCurrentTimeMilliseconds() + " _refresh_interval:" + _refresh_interval);
            _last_post_newlocation = _time.getCurrentTimeMilliseconds();
            _bus.post(event);
        }

        if (_advancedLocation.getAltitude() != 0.0) {
            _altitudeGraphReduce.addAltitude((int) _advancedLocation.getAltitude(), _advancedLocation.getElapsedTime(), _advancedLocation.getDistance());

            NewAltitude newAltitude = new NewAltitude(_altitudeGraphReduce.getGraphData());

            _bus.post(newAltitude);

            _savedNewAltitude = newAltitude;
        }
    }

    private void broadcastStatus(BaseStatus.Status currentStatus) {
        //Log.d(TAG, "broadcastStatus:" + currentStatus.toString());
        _bus.post(new GPSStatus(currentStatus));
        if (_savedLocation != null) {
            Log.d(TAG, "broadcastStatus: rebroadcast _savedLocation");
            _bus.post(_savedLocation);
        }
        if (_savedNewAltitude != null) {
            Log.d(TAG, "broadcastStatus: rebroadcast _savedNewAltitude");
            _bus.post(_savedNewAltitude);
        }
    }
}