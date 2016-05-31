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
import com.njackson.adapters.NewLocationToSavedLocation;
import com.njackson.application.IInjectionContainer;
import com.njackson.application.modules.ForApplication;
import com.njackson.events.BleServiceCommand.BleSensorData;
import com.njackson.events.GPSServiceCommand.ChangeRefreshInterval;
import com.njackson.events.GPSServiceCommand.GPSChangeState;
import com.njackson.events.GPSServiceCommand.GPSStatus;
import com.njackson.events.GPSServiceCommand.NewAltitude;
import com.njackson.events.GPSServiceCommand.ResetGPSState;
import com.njackson.events.GPSServiceCommand.NewLocation;
import com.njackson.events.GPSServiceCommand.SavedLocation;
import com.njackson.events.base.BaseStatus;
import com.njackson.service.IServiceCommand;
import com.njackson.state.IGPSDataStore;
import com.njackson.strava.StravaUpload;
import com.njackson.utils.AltitudeGraphReduce;
import com.njackson.utils.BatteryStatus;
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
    private int _cyclingCadence = 0;
    private int _runningCadence = 0;
    private double _temperature = 0;
    private int _batteryLevel = 0;
    private BaseStatus.Status _currentStatus= BaseStatus.Status.NOT_INITIALIZED;
    private SavedLocation _savedLocation = null;
    private NewAltitude _savedNewAltitude = null;
    private int _nbLocationReceived = 0;

    private long _last_post_newlocation  = 0;
    private long _last_post_battery_level  = 0;
    private long _last_post_temperature  = 0;
    private long _last_post_hr_max  = 0;

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
    public void onNewBleSensorData(BleSensorData event) {
        switch (event.getType()) {
            case BleSensorData.SENSOR_HRM:
                _heartRate = event.getHeartRate();
                Log.d(TAG, "onNewBleSensorData _heartRate:" + _heartRate);
                break;
            case BleSensorData.SENSOR_CSC_CADENCE:
                _cyclingCadence = event.getCyclingCadence();
                Log.d(TAG, "onNewBleSensorData _cadence:" + _cyclingCadence);
                break;
            case BleSensorData.SENSOR_CSC_WHEEL_RPM:
                int wheelSize;
                try {
                    wheelSize = Integer.valueOf(_sharedPreferences.getString("PREF_BLE_CSC_WHEEL_SIZE", "0"));
                } catch (Exception ex) {
                    wheelSize = 0;
                }
                if (wheelSize > 0) {
                    _advancedLocation.setSensorSpeed(wheelSize / 1000 * event.getCyclingWheelRpm() / 60, _time.getCurrentTimeMilliseconds());
                }
                Log.d(TAG, "onNewBleSensorData wheelRpm:" + event.getCyclingWheelRpm() + " wheelSize:" + wheelSize);
                break;
            case BleSensorData.SENSOR_RSC:
                _runningCadence = event.getRunningCadence();
                Log.d(TAG, "onNewBleSensorData _runningCadence:" + _runningCadence);
                break;

            case BleSensorData.SENSOR_TEMPERATURE:
                _temperature = event.getTemperature();
                Log.d(TAG, "onNewBleSensorData _temperature:" + _temperature);
                break;
            default:
                Log.d(TAG, "onNewBleSensorData type unknown:" + event.getType());
                break;
        }
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

        StravaUpload strava_upload = new StravaUpload(_applicationContext);
        strava_upload.upload(_sharedPreferences.getString("strava_token", ""));
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
        _advancedLocation.setAltitudeCalibrationDelta(_dataStore.getAltitudeCalibrationDelta(_time.getCurrentTimeMilliseconds()));

        if (_dataStore.getFirstLocationLattitude() != 0.0f && _dataStore.getFirstLocationLongitude() != 0.0f) {
            firstLocation = new Location("Ventoo");
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
        _dataStore.setAltitudeCalibrationDelta((float) _advancedLocation.getAltitudeCalibrationDelta(), _time.getCurrentTimeMilliseconds());
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
        _locationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, refresh_interval % 100000, 2.0f, _locationListener);
    }

    private void registerNmeaListener() {
        _nmeaListener = new ServiceNmeaListener(_advancedLocation,_locationMgr, _dataStore);
        _locationMgr.addNmeaListener(_nmeaListener);
    }

    private void registerSensorListener() {
        _sensorListener = new GPSSensorEventListener(_advancedLocation,_sensorManager,new Callable() {
            @Override
            public Object call() throws Exception {
                //Log.d(TAG, "getAltitudeFromPressure:" + _advancedLocation.getAltitudeFromPressure());
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
            _advancedLocation.onLocationChanged(location, _heartRate, _cyclingCadence);
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
    private NewLocation previousLocation;
    private int nbSent=0;
    private int previousHeartRateMax = 0;
    private void broadcastLocation(Location location) {
        boolean force_send = false;
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

        try {
            int heartRateMax = Integer.valueOf(_sharedPreferences.getString("PREF_BLE_HRM_HRMAX", "0"));
            if (heartRateMax != previousHeartRateMax || _time.getCurrentTimeMilliseconds() - _last_post_hr_max > 5 * 60 * 1000) {
                //Log.d(TAG, "previousHeartRateMax:" + previousHeartRateMax + " heartRateMax:" + heartRateMax);
                // ||: force send every x sec
                event.setHeartRateMax(heartRateMax);
                event.setHeartRateMode(Integer.valueOf(_sharedPreferences.getString("PREF_BLE_HRM_ZONE_NOTIFICATION_MODE", "0")));
                previousHeartRateMax = heartRateMax;
                _last_post_hr_max = _time.getCurrentTimeMilliseconds();
                force_send = true;
            }
        } catch (NumberFormatException nfe) {}
        if (_cyclingCadence > 0) {
            event.setCyclingCadence(_cyclingCadence);
        }
        if (_runningCadence > 0) {
            event.setRunningCadence(_runningCadence);
        }
        if (_temperature != 0 && _time.getCurrentTimeMilliseconds() - _last_post_temperature > 60 * 1000) {
            // only send temperature if available and once every X seconds
            double temperature = _temperature;
            if (units == Constants.IMPERIAL || units == Constants.NAUTICAL_IMPERIAL || units == Constants.RUNNING_IMPERIAL) {
                // force conversion to Fahrenheit
                temperature = _temperature * 9 / 5.0 + 32;
            }
            event.setTemperature(temperature);
            _last_post_temperature = _time.getCurrentTimeMilliseconds();
            force_send = true;
        }
        if (_time.getCurrentTimeMilliseconds() - _last_post_battery_level > 60 * 1000) {
            // only send battery level once every X seconds
            _batteryLevel = BatteryStatus.getBatteryLevel(_applicationContext);
            event.setBatteryLevel(_batteryLevel);
            _last_post_battery_level = _time.getCurrentTimeMilliseconds();
            force_send = true;
        }

        _savedLocation = new NewLocationToSavedLocation(event);

        if (locationShouldBeSended(_advancedLocation, force_send)) {
            // 0.95 to avoid skipping wanted data
            //Log.d(TAG, "ts:" + _time.getCurrentTimeMilliseconds() + " _refresh_interval:" + _refresh_interval);
            _last_post_newlocation = _time.getCurrentTimeMilliseconds();
            previousLocation = event;

            nbSent++;
            //event.setAscentRate(nbSent);
            _bus.post(event);
        }

        if (_advancedLocation.getAltitude() != 0.0) {
            _altitudeGraphReduce.addAltitude((int) _advancedLocation.getAltitude(), _advancedLocation.getElapsedTime(), _advancedLocation.getDistance());

            NewAltitude newAltitude = new NewAltitude(_altitudeGraphReduce.getGraphData());

            _bus.post(newAltitude);

            _savedNewAltitude = newAltitude;
        }
    }
    private long m_sentElapsedTime = -1;
    private double m_sentAltitude;
    //private float m_sentAccuracy;
    private float m_sentDistance;
    private float m_sentSpeed;
    private int m_sentHeartRate;
    private boolean locationShouldBeSended(AdvancedLocation p_advancedLocation, boolean p_forceSsend) {

        boolean send = p_forceSsend;

        //Log.d(TAG, "locationShouldBeSended _refresh_interval%100000="+_refresh_interval % 100000+" _refresh_interval=" + _refresh_interval);

        if (_time.getCurrentTimeMilliseconds() - _last_post_newlocation > (_refresh_interval % 100000) * 0.95) {
            // _refresh_interval % 100000 = GPS minTime
            // 0.95 to avoid skipping wanted data
            //Log.d(TAG, "ts:" + _time.getCurrentTimeMilliseconds() + " _refresh_interval:" + _refresh_interval);

            int adaptativeMode = _refresh_interval/100000;
            Log.d(TAG, "adaptativeMode:" + adaptativeMode);

            if (adaptativeMode == 0) {
                // standard mode
                send = true;
            } else if (m_sentElapsedTime < 0) {
                // adaptative mode, no previous location
                send = true;
            } else {
                // adaptative mode

                double deltaAltitude = Math.abs(Math.floor(p_advancedLocation.getAltitude()) - Math.floor(m_sentAltitude)); // in m
//                double deltaAccuracy = Math.abs(Math.floor(p_advancedLocation.getAccuracy()) - Math.floor(m_sentAccuracy)); // in m
                double deltaDistance = Math.abs(Math.floor( p_advancedLocation.getDistance() / 100) - Math.floor(m_sentDistance / 100)) / 10; // in km (delta of distances floored to 100m)
//                Log.d(TAG, "p_advancedLocation.getDistance()=" + p_advancedLocation.getDistance() + " m_sentDistance=" + m_sentDistance);
                double deltaSpeed = Math.abs(Math.floor(p_advancedLocation.getSpeed()*3.6) - Math.floor(m_sentSpeed*3.6)); // in km/h (rounded at 1 km/h)
                double averageSpeed = p_advancedLocation.getElapsedTime() > 0 ? p_advancedLocation.getDistance() * 3.6 / (p_advancedLocation.getElapsedTime()/1000) : 0; // in km/h
                int deltaHeartRate = Math.abs(_heartRate - m_sentHeartRate);
                double minDeltaAltitude;
                double minDeltaDistance;
                double minDeltaSpeed;
                double minDeltaHeartrate;
                switch (adaptativeMode) {
                    case 1:
                        // high - normal
                        minDeltaAltitude = 5; // 5m => 18s at 1000m/h
                        minDeltaDistance = averageSpeed / 3600 * 10; // in km, distance traveled at average speed during 10 s
                        minDeltaSpeed = 0.2 * averageSpeed; // 20% of average speed
                        minDeltaHeartrate = 5;
                        break;
                    case 2:
                        // medium
                        minDeltaAltitude = 10;
                        minDeltaDistance = averageSpeed / 3600 * 20; // in km, distance traveled at average speed during 20 s
                        minDeltaSpeed = 0.3 * averageSpeed; // 30% of average speed
                        minDeltaHeartrate = 10;
                        break;
                    case 3:
                        // low
                        minDeltaAltitude = 20;
                        minDeltaDistance = 2 * averageSpeed / 3600 * 30; // in km, distance traveled at 2x average speed during 30 s
                        minDeltaSpeed = 0.4 * averageSpeed; // 40% of average speed
                        minDeltaHeartrate = 20;
                        break;
                    default:
                        minDeltaAltitude = 5; // m
                        minDeltaDistance = 0.5; // km
                        minDeltaSpeed = 3; // km/h
                        minDeltaHeartrate = 10;
                }
                minDeltaAltitude = Math.max(minDeltaAltitude, 5); // m
                minDeltaDistance = Math.max(minDeltaDistance, 0.1); // km/h
                minDeltaSpeed = Math.max(minDeltaSpeed, 3); // km/h
                Log.d(TAG, " minDeltaAltitude:" + minDeltaAltitude + " minDeltaDistance:" + minDeltaDistance + " minDeltaSpeed:" + minDeltaSpeed);
                Log.d(TAG, " deltaAltitude:" + deltaAltitude + " deltaDistance:" + deltaDistance + " deltaSpeed:" + deltaSpeed + " averageSpeed:" + averageSpeed);
                if (deltaAltitude >= minDeltaAltitude && deltaAltitude > 3 * p_advancedLocation.getAccuracy()) {
                    Log.d(TAG, "sent forced by altitude deltaAltitude:" + deltaAltitude + " > " + minDeltaAltitude);
                    send = true;
                }
                if (deltaDistance >= minDeltaDistance) {
                    Log.d(TAG, "sent forced by distance deltaDistance:" + deltaDistance + " > " + minDeltaDistance);
                    send = true;
                }
                if (p_advancedLocation.getSpeed() >= 1 /* m/s */ && deltaSpeed >= minDeltaSpeed) {
                    Log.d(TAG, "sent forced by speed deltaSpeed:" + deltaSpeed + " > " + minDeltaSpeed);
                    send = true;
                }
                if (deltaHeartRate >= minDeltaHeartrate) {
                    Log.d(TAG, "sent forced by heartrate deltaHeartRate:" + deltaHeartRate + " > " + minDeltaHeartrate);
                    send = true;
                }

                if (_time.getCurrentTimeMilliseconds() - _last_post_newlocation > 30000) {
                    Log.d(TAG, "sent forced after 30s");
                    send = true;
                }
            }
        }
        if (send) {
            m_sentElapsedTime = _advancedLocation.getElapsedTime();
            m_sentAltitude = _advancedLocation.getAltitude();
            //m_sentAccuracy = _advancedLocation.getAccuracy();
            m_sentDistance = _advancedLocation.getDistance();
            m_sentSpeed = _advancedLocation.getSpeed();
            m_sentHeartRate = _heartRate;
        }
        return send;
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