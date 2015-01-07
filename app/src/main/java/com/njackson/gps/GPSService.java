package com.njackson.gps;

import android.app.Notification;
import android.app.PendingIntent;
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
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.njackson.R;
import com.njackson.activities.MainActivity;
import com.njackson.application.PebbleBikeApplication;
import com.njackson.events.GPSService.ChangeRefreshInterval;
import com.njackson.events.GPSService.ResetGPSState;
import com.njackson.events.GPSService.CurrentState;
import com.njackson.events.GPSService.NewLocation;
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
    @Inject IGPSServiceStarterForeground _serviceStarter;

    @Inject SharedPreferences _sharedPreferences;
    @Inject Bus _bus;

    //Location firstLocation = null;

    private AdvancedLocation _advancedLocation;
    private ServiceNmeaListener _nmeaListener;
    private GPSSensorEventListener _sensorListener;

    private int _refresh_interval = 1000;
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
        Log.d(TAG, "Stopped GPS Service");
        saveGPSStats();
        stopLocationUpdates();

        _bus.post(new CurrentState(CurrentState.State.STOPPED));

        _serviceStarter.stopServiceForeground(this);

        _bus.unregister(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleCommand(Intent intent) {
        Log.d(TAG, "Started GPS Service");

        _advancedLocation = new AdvancedLocation(getApplicationContext());
        _advancedLocation.debugTagPrefix = "PB-";

        // the intent has an extra which relates to the refresh interval
        _refresh_interval = intent.getIntExtra("REFRESH_INTERVAL", 1000);

        loadGPSStats();

        // check to see if GPS is enabled
        if(checkGPSEnabled(_locationMgr)) {
            requestLocationUpdates(_refresh_interval);
            registerNmeaListener();
            registerSensorListener();

            _bus.post(new CurrentState(CurrentState.State.STARTED));

            // broadcast the saved values directly
            NewLocation event = new NewLocation();
            try {
                event.setUnits(Integer.valueOf(_sharedPreferences.getString("UNITS_OF_MEASURE", "0")));
            } catch (NumberFormatException e) {
                event.setUnits(0);
            }
            event.setDistance(_advancedLocation.getDistance());
            event.setAvgSpeed(_advancedLocation.getAverageSpeed());
            event.setAscent(_advancedLocation.getAscent()); // m
            _bus.post(event);
        } else {
            _bus.post(new CurrentState(CurrentState.State.DISABLED)); // GPS DISABLED
        }
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
    }

    // save the state
    private void saveGPSStats() {
        SharedPreferences.Editor editor = _sharedPreferences.edit();
        editor.putFloat("GPS_DISTANCE",_advancedLocation.getDistance());
        editor.putLong("GPS_ELAPSEDTIME", _advancedLocation.getElapsedTime());
        editor.putFloat("GPS_ASCENT", (float) _advancedLocation.getAscent());
        editor.putFloat("GEOID_HEIGHT", (float) _advancedLocation.getGeoidHeight());
        editor.commit();
    }

    // reset the saved state
    private void resetGPSStats() {
        SharedPreferences.Editor editor = _sharedPreferences.edit();
        editor.putFloat("GPS_DISTANCE",0.0f);
        editor.putLong("GPS_ELAPSEDTIME", 0);
        editor.putFloat("GPS_ASCENT", 0.0f);
        editor.commit();

        // GPS is running
        // reninit all properties
        _advancedLocation = new AdvancedLocation(getApplicationContext());
        _advancedLocation.debugTagPrefix = "PB-";

        loadGPSStats();
    }

    private void requestLocationUpdates(int refresh_interval) {
        _refresh_interval = refresh_interval;

        if (_gpsStarted) {
            _locationMgr.removeUpdates(_locationListener);
        }
        _locationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, (long)_refresh_interval, 2.0f, _locationListener);

        _gpsStarted = true;
    }

    private void registerNmeaListener() {
        _nmeaListener = new ServiceNmeaListener(_advancedLocation);
        _locationMgr.addNmeaListener(_nmeaListener);
    }

    private void registerSensorListener() {
        _sensorListener = new GPSSensorEventListener(_advancedLocation,_sensorManager,new Callable() {
            @Override
            public Object call() throws Exception {
                broadcastLocation();
                return null;
            }
        });

        _sensorManager.registerListener(_sensorListener,_sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE),SensorManager.SENSOR_DELAY_NORMAL);
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
            broadcastLocation();
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

    private void broadcastLocation() {
        NewLocation event = new NewLocation();
        try {
            event.setUnits(Integer.valueOf(_sharedPreferences.getString("UNITS_OF_MEASURE", "0")));
        } catch (NumberFormatException e) {
            event.setUnits(0);
        }
        event.setSpeed(_advancedLocation.getSpeed());
        event.setDistance(_advancedLocation.getDistance());
        event.setAvgSpeed(_advancedLocation.getAverageSpeed());
        event.setLatitude(_advancedLocation.getLatitude());
        event.setLongitude(_advancedLocation.getLongitude());
        event.setAltitude(_advancedLocation.getAltitude()); // m
        event.setAscent(_advancedLocation.getAscent()); // m
        event.setAscentRate(3600f * _advancedLocation.getAscentRate()); // in m/h
        event.setSlope(100f * _advancedLocation.getSlope()); // in %
        event.setAccuracy(_advancedLocation.getAccuracy()); // m
        event.setTime(_advancedLocation.getTime());
        event.setElapsedTimeSeconds(_advancedLocation.getElapsedTime());
        //event.setXpos(_advancedLocation.get);
        //event.setYpos(ypos);
        event.setBearing(_advancedLocation.getBearing());

        _bus.post(event);
        Log.d(TAG,"New Location");
    }

}