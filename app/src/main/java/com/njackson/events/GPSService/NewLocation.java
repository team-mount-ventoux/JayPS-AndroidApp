package com.njackson.events.GPSService;

/**
 * Created by server on 21/03/2014.
 */
public class NewLocation {

    private float _speed;
    public float getSpeed() {
        return _speed;
    }
    public void setSpeed(float value) {
        _speed = value;
    }

    private float _distance;
    public float getDistance() {
        return _distance;
    }
    public void setDistance(float _distance) {
        this._distance = _distance;
    }

    private float _avgSpeed;
    public float getAvgSpeed() {
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

    private long _elapsedTimeSeconds;
    public long getElapsedTimeSeconds() {
        return _elapsedTimeSeconds;
    }
    public void setElapsedTimeSeconds(long _time) {
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
}
