package com.njackson;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

public class BatteryService extends Service {
    
    private static final String TAG = "PB-BatteryService";

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryLevelReceiver, batteryLevelFilter);
        
        return START_STICKY;
    }
    BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            int rawlevel = intent.getIntExtra("level", -1);
            int scale = intent.getIntExtra("scale", -1);
            if (rawlevel >= 0 && scale > 0) {
                MainActivity.batteryLevel = (rawlevel * 100) / scale;
                MainActivity.sendBatteryLevel();
            }
            if (MainActivity.debug) Log.d(TAG, "battery rawlevel:" + rawlevel + " scale:" + scale + " batteryLevel:" + MainActivity.batteryLevel);
         }
    };
}
