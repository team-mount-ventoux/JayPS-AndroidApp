package com.njackson.events.BleServiceCommand;

/**
 * Created by jay on 25/01/15.
 */
public class BleHeartRate {
    private int _heartRate = 0;
    public int getHeartRate() {
        return _heartRate;
    }
    public void setHeartRate(int heartRate) {
        this._heartRate = heartRate;
    }


    public BleHeartRate(int heartRate) {
        _heartRate = heartRate;
    }
}
