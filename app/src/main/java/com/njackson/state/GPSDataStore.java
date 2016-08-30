package com.njackson.state;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.njackson.Constants;
import com.njackson.R;

/**
 * Created by njackson on 30/01/15.
 */
public class GPSDataStore implements IGPSDataStore {

    private static final String TAG = "PB-GPSDataStore";

    SharedPreferences _sharedPreferences;
    Context _context;

    long _startTime = 0;
    long _prevStartTime = 0;
    float _distance = 0;
    long _elapsedTime = 0;
    float _ascent = 0;
    int _nbascent = 0;
    float _maxSpeed = 0;
    private float _altitudeCalibrationDelta;
    private long _altitudeCalibrationDeltaTime;
    private float _geoid;
    private float _latitude;
    private float _longitude;
    private float _lastLatitude;
    private float _lastLongitude;
    private int _units;


    public GPSDataStore(SharedPreferences preferences, Context context) {

        _sharedPreferences = preferences;
        _context = context;
        loadSavedData();

    }

    public void reloadPreferencesFromSettings() {
        try {
            _units = Integer.valueOf(_sharedPreferences.getString("UNITS_OF_MEASURE", "" + Constants.METRIC));
        } catch (Exception ex) {
            _units = Constants.METRIC;
        }
    }
    private void loadSavedData() {
        reloadPreferencesFromSettings();

        _startTime = _sharedPreferences.getLong("GPS_LAST_START",0);
        _distance = _sharedPreferences.getFloat("GPS_DISTANCE", 0);
        _elapsedTime = _sharedPreferences.getLong("GPS_ELAPSEDTIME",0);
        _ascent = _sharedPreferences.getFloat("GPS_ASCENT", 0);
        _nbascent = _sharedPreferences.getInt("GPS_NB_ASCENT", 0);
        _maxSpeed = _sharedPreferences.getFloat("GPS_MAX_SPEED", 0);
        _altitudeCalibrationDelta = _sharedPreferences.getFloat("ALTITUDE_CALIBRATION_DELTA", 0);
        _altitudeCalibrationDeltaTime = _sharedPreferences.getLong("ALTITUDE_CALIBRATION_DELTA_TIME",0);
        _geoid = _sharedPreferences.getFloat("GEOID_HEIGHT",0);
        _latitude = _sharedPreferences.getFloat("GPS_FIRST_LOCATION_LAT",0);
        _longitude = _sharedPreferences.getFloat("GPS_FIRST_LOCATION_LON",0);
        _lastLatitude = _sharedPreferences.getFloat("GPS_LAST_LOCATION_LAT",0);
        _lastLongitude = _sharedPreferences.getFloat("GPS_LAST_LOCATION_LON",0);
    }

    @Override
    public int getMeasurementUnits() {
        return _units;
    }

    @Override
    public void setMeasurementUnits(int value) {
        _units = value;
    }

    @Override
    public long getStartTime() {
        return _startTime;
    }
    @Override
    public long getPrevStartTime() {
        return _prevStartTime;
    }

    @Override
    public void setStartTime(long value) {
        _prevStartTime = _startTime;
        _startTime = value;
    }

    @Override
    public float getDistance() {
        return _distance;
    }

    @Override
    public void setDistance(float value) {
        _distance = value;
    }

    @Override
    public long getElapsedTime() {
        return _elapsedTime;
    }

    @Override
    public void setElapsedTime(long value) {
        _elapsedTime = value;
    }

    @Override
    public float getAscent() {
        return _ascent;
    }

    @Override
    public void setAscent(float value) {
        _ascent = value;
    }

    @Override
    public int getNbAscent() {
        return _nbascent;
    }

    @Override
    public void setNbAscent(int value) {
        _nbascent = value;
    }

    @Override
    public float getMaxSpeed() {
        return _maxSpeed;
    }

    @Override
    public void setMaxSpeed(float value) {
        _maxSpeed = value;
    }

    @Override
    public float getAltitudeCalibrationDelta(long time) {
        if (time - _altitudeCalibrationDeltaTime < 1000 * 3600) {
            // last saved value is not older than X seconds
            return _altitudeCalibrationDelta;
        } else {
            return 0;
        }
    }

    @Override
    public void setAltitudeCalibrationDelta(float value, long time)
    {
        _altitudeCalibrationDelta = value;
        _altitudeCalibrationDeltaTime = time;
    }

    @Override
    public float getGEOIDHeight() {
        return _geoid;
    }

    @Override
    public void setGEOIDHeight(float value) {
        _geoid = value;
    }

    @Override
    public float getFirstLocationLattitude() {
        return _latitude;
    }

    @Override
    public void setFirstLocationLattitude(float value) {
        _latitude = value;
    }

    @Override
    public float getFirstLocationLongitude() {
        return _longitude;
    }

    @Override
    public void setFirstLocationLongitude(float value) {
        _longitude = value;
    }

    @Override
    public float getLastLocationLatitude() {
        return _lastLatitude;
    }

    @Override
    public void setLastLocationLatitude(float value) {
        _lastLatitude = value;
    }

    @Override
    public float getLastLocationLongitude() {
        return _lastLongitude;
    }

    @Override
    public void setLastLocationLongitude(float value) {
        _lastLongitude = value;
    }


    @Override
    public void resetAllValues() {
        //_startTime = 0; // no reset needed, it's for orxumaps auto-start
        _distance = 0;
        _elapsedTime = 0;
        _ascent = 0;
        _nbascent = 0;
        _maxSpeed = 0;
        //_altitudeCalibrationDelta = 0; // no reset needed, it's for altitude correction
        //_geoid = 0; // no reset needed, it's for altitude correction
        _latitude = 0;
        _longitude = 0;

        // TODO(nic) move me to some other place?
        SharedPreferences.Editor editor = _sharedPreferences.edit();

        editor.putString("SPEEDFRAGMENT_SPEED", _context.getString(R.string.speedfragment_speed_value));
        editor.putString("SPEEDFRAGMENT_AVGSPEED", _context.getString(R.string.speedfragment_avgspeed_value));
        editor.putString("SPEEDFRAGMENT_DISTANCE", _context.getString(R.string.speedfragment_distance_value));
        editor.putString("SPEEDFRAGMENT_TIME", _context.getString(R.string.speedfragment_time_value));

        editor.commit();
    }

    @Override
    public void commit() {
        SharedPreferences.Editor editor = _sharedPreferences.edit();

        editor.putString("UNITS_OF_MEASURE", "" +  _units);
        editor.putLong("GPS_LAST_START",_startTime);
        editor.putFloat("GPS_DISTANCE",_distance);
        editor.putLong("GPS_ELAPSEDTIME", _elapsedTime);
        editor.putFloat("GPS_ASCENT", _ascent);
        editor.putInt("GPS_NB_ASCENT", _nbascent);
        editor.putFloat("GPS_MAX_SPEED", _maxSpeed);
        editor.putFloat("ALTITUDE_CALIBRATION_DELTA", _altitudeCalibrationDelta);
        editor.putLong("ALTITUDE_CALIBRATION_DELTA_TIME", _altitudeCalibrationDeltaTime);
        editor.putFloat("GEOID_HEIGHT", _geoid);
        editor.putFloat("GPS_FIRST_LOCATION_LAT", _latitude);
        editor.putFloat("GPS_FIRST_LOCATION_LON", _longitude);
        editor.putFloat("GPS_LAST_LOCATION_LAT", _lastLatitude);
        editor.putFloat("GPS_LAST_LOCATION_LON", _lastLongitude);

        editor.commit();
    }

}
