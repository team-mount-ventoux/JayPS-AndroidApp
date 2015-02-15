package com.njackson.hrm;

import android.util.Log;

public class Csc {
    private final String TAG = "PB-Csc";

    private int prevWheelEventTime = -1;
    private int prevCrankEventTime = -1;
    private int prevCumulativeWheelRevolutions = -1;
    private int prevCumulativeCrankRevolutions = -1;
    private float wheelRpm = 0;
    private float crankRpm = 0;

    public float getWheelRpm() {
        return wheelRpm;
    }
    public float getCrankRpm() {
        return crankRpm;
    }

    public void onNewValues(int cumulativeWheelRevolutions, int lastWheelEventTime, int cumulativeCrankRevolutions, int lastCrankEventTime) {
        Log.d(TAG, "onNewValues(" + cumulativeWheelRevolutions + ", " + lastWheelEventTime + ", " + cumulativeCrankRevolutions + ", " + lastCrankEventTime + ")");

        if (prevWheelEventTime > -1) {
            if (lastWheelEventTime != prevWheelEventTime) {
                wheelRpm = 60 * 1024f / ((lastWheelEventTime - prevWheelEventTime + 65536) % 65536);
                if (prevCumulativeWheelRevolutions > -1 && cumulativeWheelRevolutions > prevCumulativeWheelRevolutions + 1) {
                    wheelRpm = wheelRpm * (cumulativeWheelRevolutions - prevCumulativeWheelRevolutions);
                    Log.v(TAG, "wheelRpm2: " + wheelRpm);
                }
                Log.i(TAG, "wheelRpm: " + wheelRpm);
                Log.v(TAG, "ratio: " + (crankRpm > 0 ? wheelRpm / crankRpm : 0));
            }
        }
        if (prevCrankEventTime > -1) {
            if (lastCrankEventTime != prevCrankEventTime) {
                crankRpm = 60 * 1024f / ((lastCrankEventTime - prevCrankEventTime + 65536) % 65536);
                if (prevCumulativeCrankRevolutions > -1 && cumulativeCrankRevolutions > prevCumulativeCrankRevolutions + 1) {
                    crankRpm = crankRpm * (cumulativeCrankRevolutions - prevCumulativeCrankRevolutions);
                    Log.v(TAG, "crankRpm2: " + crankRpm);
                }
                Log.i(TAG, "crankRpm: " + crankRpm);
            }
        }

        prevCumulativeWheelRevolutions = cumulativeWheelRevolutions;
        prevWheelEventTime = lastWheelEventTime;
        prevCumulativeCrankRevolutions = cumulativeCrankRevolutions;
        prevCrankEventTime = lastCrankEventTime;
    }
}
