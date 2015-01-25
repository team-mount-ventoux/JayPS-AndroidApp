package com.njackson.events.HrmServiceCommand;

/**
 * Created by jay on 25/01/15.
 */
public class HrmHeartRate {
    private int _heartRate = 0;
    public int getHeartRate() {
        return _heartRate;
    }
    public void setHeartRate(int heartRate) {
        this._heartRate = _heartRate;
    }


    public HrmHeartRate(int heartRate) {
        _heartRate = heartRate;
    }
}
