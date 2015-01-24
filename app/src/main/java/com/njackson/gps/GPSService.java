package com.njackson.gps;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;

import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.njackson.adapters.AdvancedLocationToNewLocation;
import com.njackson.application.PebbleBikeApplication;
import com.njackson.events.GPSService.ChangeRefreshInterval;
import com.njackson.events.status.GPSStatus;
import com.njackson.events.GPSService.ResetGPSState;
import com.njackson.events.GPSService.NewLocation;
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
public class GPSService extends Service {

    private static final String TAG = "PB-GPSService";

    @Inject LocationManager _locationMgr;
    @Inject SensorManager _sensorManager;
    @Inject
    IForegroundServiceStarter _serviceStarter;
    @Inject SharedPreferences _sharedPreferences;
    @Inject Bus _bus;
    @Inject ITime _time;

    private AdvancedLocation _advancedLocation;
    private Location firstLocation = null;
    private ServiceNmeaListener _nmeaListener;
    private GPSSensorEventListener _sensorListener;

    private long _refresh_interval = 1000;
    private boolean _gpsStarted = false;

    @Subscribe
    public void onResetGPSStateEvent(ResetGPSState event) {
        //stop service stopLocationUpdates();
        resetGPSStats();
    }

    @Subscribe
    public void onGPSRefreshChangeEvent(ChangeRefreshInterval event) {
        changeRefreshInterval(event.getRefreshInterval());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Started GPS Service");

        handleCommand(intent);
        _serviceStarter.startServiceForeground(this, "Pebble Bike", "GPS started");

        // ensures that if the service is recycled then it is restarted with the same refresh interval
        // onStartCommand will always be called with a non-null intent
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        ((PebbleBikeApplication)getApplication()).inject(this);
        _bus.register(this);
    }

    @Override
    public void onDestroy (){
        Log.d(TAG, "Destroy GPS Service");
        saveGPSStats();

        stopLocationUpdates();

        _serviceStarter.stopServiceForeground(this);

        _bus.post(new GPSStatus(GPSStatus.State.STOPPED));

        _bus.unregister(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleCommand(Intent intent) {
        _advancedLocation = new AdvancedLocation(getApplicationContext());
        _advancedLocation.debugLevel = 0; //debug ? 2 : 0;
        _advancedLocation.debugTagPrefix = "PB-";

        // the intent has an extra which relates to the refresh interval
        _refresh_interval = intent.getIntExtra("REFRESH_INTERVAL", 1000);

        loadGPSStats();

        // check to see if GPS is enabled
        if(checkGPSEnabled(_locationMgr)) {
            requestLocationUpdates(_refresh_interval);
            registerNmeaListener();
            registerSensorListener();
            setGPSStartTime();

            _bus.post(new GPSStatus(GPSStatus.State.STARTED));
        } else {
            _bus.post(new GPSStatus(GPSStatus.State.DISABLED)); // GPS DISABLED
        }
    }

    private void setGPSStartTime() {
        SharedPreferences.Editor editor = _sharedPreferences.edit();
        editor.putLong("GPS_LAST_START", _time.getCurrentTimeMilliseconds());
        editor.commit();
    }

    private boolean checkGPSEnabled(LocationManager locationMgr) {
        return locationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    // load the saved state
    private void loadGPSStats() {
        Log.d(TAG, "loadGPSStats()");

        _advancedLocation.setDistance(_sharedPreferences.getFloat("GPS_DISTANCE", 0.0f));
        _advancedLocation.setElapsedTime(_sharedPreferences.getLong("GPS_ELAPSEDTIME", 0));

        try {
            _advancedLocation.setAscent(_sharedPreferences.getFloat("GPS_ASCENT", 0.0f));
        } catch (ClassCastException e) {
            _advancedLocation.setAscent(0.0);
        }

        _advancedLocation.setGeoidHeight(_sharedPreferences.getFloat("GEOID_HEIGHT", 0));

        if (_sharedPreferences.contains("GPS_FIRST_LOCATION_LAT") && _sharedPreferences.contains("GPS_FIRST_LOCATION_LON")) {
            firstLocation = new Location("PebbleBike");
            firstLocation.setLatitude(_sharedPreferences.getFloat("GPS_FIRST_LOCATION_LAT", 0.0f));
            firstLocation.setLongitude(_sharedPreferences.getFloat("GPS_FIRST_LOCATION_LON", 0.0f));
        } else {
            firstLocation = null;
        }
    }

    // save the state
    private void saveGPSStats() {
        SharedPreferences.Editor editor = _sharedPreferences.edit();
        editor.putFloat("GPS_DISTANCE",_advancedLocation.getDistance());
        editor.putLong("GPS_ELAPSEDTIME", _advancedLocation.getElapsedTime());
        editor.putFloat("GPS_ASCENT", (float) _advancedLocation.getAscent());
        editor.putFloat("GEOID_HEIGHT", (float) _advancedLocation.getGeoidHeight());
        if (firstLocation != null) {
            editor.putFloat("GPS_FIRST_LOCATION_LAT", (float) firstLocation.getLatitude());
            editor.putFloat("GPS_FIRST_LOCATION_LON", (float) firstLocation.getLongitude());
        }
        editor.commit();
    }

    // reset the saved state
    private void resetGPSStats() {
        SharedPreferences.Editor editor = _sharedPreferences.edit();
        editor.putFloat("GPS_DISTANCE", 0.0f);
        editor.putLong("GPS_ELAPSEDTIME", 0);
        editor.putFloat("GPS_ASCENT", 0.0f);
        editor.commit();

        // GPS is running
        // reninit all properties
        _advancedLocation = new AdvancedLocation(getApplicationContext());
        _advancedLocation.debugLevel = 0; //debug ? 2 : 0;
        _advancedLocation.debugTagPrefix = "PB-";

        loadGPSStats();
    }

    private void requestLocationUpdates(long refresh_interval) {
        _refresh_interval = refresh_interval;

        if (_gpsStarted) {
            _locationMgr.removeUpdates(_locationListener);
        }
        _locationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, _refresh_interval, 2.0f, _locationListener);

        _gpsStarted = true;
    }

    private void registerNmeaListener() {
        _nmeaListener = new ServiceNmeaListener(_advancedLocation,_locationMgr, _sharedPreferences);
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

    private void changeRefreshInterval(int refresh_interval) {
        _refresh_interval = refresh_interval;
        requestLocationUpdates(refresh_interval);
    }

    private LocationListener _locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            _advancedLocation.onLocationChanged(location);
            if (firstLocation == null) {
                firstLocation = location;
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

        int units = 0;
        try {
            units = Integer.valueOf(_sharedPreferences.getString("UNITS_OF_MEASURE", "0"));
        } catch (NumberFormatException e) {

        }
        NewLocation event = new AdvancedLocationToNewLocation(_advancedLocation, xpos, ypos, units);

        _bus.post(event);
    }

}