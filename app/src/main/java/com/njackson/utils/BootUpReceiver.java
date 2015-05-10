package com.njackson.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.njackson.application.PebbleBikeApplication;
import com.njackson.utils.services.IServiceStarter;

import javax.inject.Inject;

public class BootUpReceiver extends BroadcastReceiver {

    private static final String TAG = "PB-BootUpReceiver";

    @Inject SharedPreferences _sharedPreferences;
    @Inject IServiceStarter _serviceStarter;

    @Override
    public void onReceive(Context context, Intent intent) {

        ((PebbleBikeApplication)context.getApplicationContext()).inject(this);

        boolean activity_start = _sharedPreferences.getBoolean("ACTIVITY_RECOGNITION",false);
        if (activity_start) {
            Log.d(TAG, "startActivityService");
            _serviceStarter.startActivityService();
        }
    }
}
