package com.njackson.events.BleServiceCommand;

public class BleSensorData {
    public static final int SENSOR_NONE = 0;
    public static final int SENSOR_HRM = 1;
    public static final int SENSOR_CSC = 2;
    public static final int SENSOR_RSC = 3;
    public static final int SENSOR_TEMPERATURE = 4;

    private int _type = SENSOR_NONE;
    public int getType() {
        return _type;
    }

    private int _heartRate = 0;
    public int getHeartRate() {
        return _heartRate;
    }
    public void setHeartRate(int heartRate) {
        this._type = SENSOR_HRM;
        this._heartRate = heartRate;
    }

    private int _cyclingCadence = 0;
    public int getCyclingCadence() {
        return _cyclingCadence;
    }
    public void setCyclingCadence(int cyclingCadence) {
        this._type = SENSOR_CSC;
        this._cyclingCadence = cyclingCadence;
    }

    private int _runningCadence = 0;
    public int getRunningCadence() {
        return _runningCadence;
    }
    public void setRunningCadence(int runningCadence) {
        this._type = SENSOR_RSC;
        this._runningCadence = runningCadence;
    }

    private double _temperature = 0;
    public double getTemperature() {
        return _temperature;
    }
    public void setTemperature(double temperature) {
        this._type = SENSOR_TEMPERATURE;
        this._temperature = temperature;
    }
}