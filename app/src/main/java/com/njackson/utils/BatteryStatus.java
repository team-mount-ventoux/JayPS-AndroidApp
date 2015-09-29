package com.njackson.utils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class BatteryStatus {
    private static String TAG = "PB-BatteryStatus";

    public static int getBatteryLevel(Context context) {
        int batteryLevel = -1;
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        int rawlevel = batteryStatus.getIntExtra("level", -1);
        int scale = batteryStatus.getIntExtra("scale", -1);
        if (rawlevel >= 0 && scale > 0) {
            batteryLevel = (rawlevel * 100) / scale;
        }
        //Log.d(TAG, "battery rawlevel:" + rawlevel + " scale:" + scale + " batteryLevel:" + batteryLevel);
        return batteryLevel;
    }
}
