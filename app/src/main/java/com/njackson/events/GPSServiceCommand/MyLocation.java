package com.njackson.events.GPSServiceCommand;

import android.location.Location;

public abstract class MyLocation {
    private float _speed;
    public float getSpeed() {
        return _speed;
    }
    public void setSpeed(float value) {
        _speed = value;
    }

    private float _maxSpeed;
    public float getMaxSpeed() {
        return _maxSpeed;
    }
    public void setMaxSpeed(float value) {
        _maxSpeed = value;
    }

    private float _distance;
    public float getDistance() {
        return _distance;
    }
    public void setDistance(float _distance) {
        this._distance = _distance;
    }

    private float _avgSpeed;
    public float getAverageSpeed() {
        return _avgSpeed;
    }
    public void setAvgSpeed(float _avgSpeed) {
        this._avgSpeed = _avgSpeed;
    }

    private double _latitude;
    public double getLatitude() {
        return _latitude;
    }
    public void setLatitude(double _latitude) {
        this._latitude = _latitude;
    }

    private double _longitude;
    public double getLongitude() {
        return _longitude;
    }
    public void setLongitude(double _longitude) {
        this._longitude = _longitude;
    }

    private double _altitude;
    public double getAltitude() {
        return _altitude;
    }
    public void setAltitude(double _altitude) {
        this._altitude = _altitude;
    }

    private double _ascent;
    public double getAscent() {
        return _ascent;
    }
    public void setAscent(double _ascent) {
        this._ascent = _ascent;
    }

    private float _ascentRate;
    public float getAscentRate() {
        return _ascentRate;
    }
    public void setAscentRate(float _ascentRate) {
        this._ascentRate = _ascentRate;
    }

    private int _nbAscent;
    public int getNbAscent() {
        return _nbAscent;
    }
    public void setNbAscent(int _nbAscent) {
        this._nbAscent = _nbAscent;
    }

    private float _slope;
    public float getSlope() {
        return _slope;
    }
    public void setSlope(float _slope) {
        this._slope = _slope;
    }

    private float _accuracy;
    public float getAccuracy() {
        return _accuracy;
    }
    public void setAccuracy(float _accuracy) {
        this._accuracy = _accuracy;
    }

    private int _elapsedTimeSeconds;
    public int getElapsedTimeSeconds() {
        return _elapsedTimeSeconds;
    }
    public void setElapsedTimeSeconds(int _time) {
        this._elapsedTimeSeconds = _time;
    }

    private double _xpos;
    public double getXpos() {
        return _xpos;
    }
    public void setXpos(double _xpos) {
        this._xpos = _xpos;
    }

    private double _ypos;
    public double getYpos() {
        return _ypos;
    }
    public void setYpos(double _ypos) {
        this._ypos = _ypos;
    }

    private double _bearing;
    public double getBearing() {
        return _bearing;
    }
    public void setBearing(double _bearing) {
        this._bearing = _bearing;
    }

    private int _units;
    public int getUnits() { return this._units; }
    public void setUnits(int units) { this._units = units;}

    private long _time;
    public void setTime(long time) { this._time = time; }
    public long getTime() { return this._time; }

    private int _HeartRate = 0;
    public int getHeartRate() { return this._HeartRate; }
    public void setHeartRate(int heartRate) { this._HeartRate = heartRate;}

    private int _HeartRateMax = 0;
    public int getHeartRateMax() { return this._HeartRateMax; }
    public void setHeartRateMax(int heartRateMax) { this._HeartRateMax = heartRateMax;}

    private int _HeartRateMode = 0;
    public int getHeartRateMode() { return this._HeartRateMode; }
    public void setHeartRateMode(int HeartRateMode) { this._HeartRateMode = HeartRateMode;}

    private int _cyclingCadence = 0;
    public int getCyclingCadence() { return this._cyclingCadence; }
    public void setCyclingCadence(int cyclingCadence) { this._cyclingCadence = cyclingCadence;}

    private int _runningCadence = 0;
    public int getRunningCadence() { return this._runningCadence; }
    public void setRunningCadence(int runningCadence) { this._runningCadence = runningCadence;}

    private double _temperature = 0;
    public double getTemperature() { return this._temperature; }
    public void setTemperature(double temperature) { this._temperature = temperature;}

    private int _batteryLevel = 0;
    public int getBatteryLevel() { return this._batteryLevel; }
    public void setBatteryLevel(int batteryLevel) { this._batteryLevel = batteryLevel;}

    private Location _firstLocation = null;
    public Location getFirstLocation() { return this._firstLocation; }
    public void setFirstLocation(Location firstLocation) { this._firstLocation = firstLocation;}


    private boolean _sendNavigation = false;
    public boolean getSendNavigation() { return this._sendNavigation; }
    public void setSendNavigation(boolean sendNavigation) { this._sendNavigation = sendNavigation;}
}
