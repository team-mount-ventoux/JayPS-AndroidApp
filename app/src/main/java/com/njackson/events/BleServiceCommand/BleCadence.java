package com.njackson.events.BleServiceCommand;

public class BleCadence {
    private int _cadence = 0;
    public int getCadence() {
        return _cadence;
    }
    public void setCadence(int cadence) {
        this._cadence = cadence;
    }


    public BleCadence(int cadence) {
        _cadence = cadence;
    }
}
