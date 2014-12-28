package com.njackson.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;

/**
 * Created by jay on 28/12/14.
 */
public class Version {
    private static final String TAG = "PB-Version";

    public static String getVersionCode(Context context) {
        if (context == null) {
            return "";
        }

        int versionCode;

        // Get current version code and version name
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);

            versionCode = packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            versionCode = 0;
        }
        Log.d(TAG, "versionCode:" + versionCode);
        return String.format("%d", versionCode);
    }

    public static String getPebbleFirmwareVersion(Context context) {
        if (context == null) {
            return "";
        }

        // Get current Pebble firmware version
        String pebbleFwVersion = "";

        PebbleKit.FirmwareVersionInfo pebbleFirmwareVersionInfo;
        int pebbleFirmwareVersion = 0;
        // try to get Pebble Watch Firmware version
        try {
            // getWatchFWVersion works only with firmware 2.x
            pebbleFirmwareVersionInfo = PebbleKit.getWatchFWVersion(context);
            pebbleFirmwareVersion = 2;
            if (pebbleFirmwareVersionInfo == null) {
                // if the watch is disconnected or we can't get the version
                Log.e(TAG, "pebbleFirmwareVersionInfo == null");
            } else {
                Log.e(TAG, "getMajor:" + pebbleFirmwareVersionInfo.getMajor());
                Log.e(TAG, "getMinor:" + pebbleFirmwareVersionInfo.getMinor());
                Log.e(TAG, "getPoint:" + pebbleFirmwareVersionInfo.getPoint());
                Log.e(TAG, "getTag:" + pebbleFirmwareVersionInfo.getTag());
            }
        } catch (Exception e) {
            //Log.e(TAG, "Exception getWatchFWVersion " + e.getMessage());
            // getWatchFWVersion works only with 2.x firmware
            pebbleFirmwareVersion = 1;
            pebbleFirmwareVersionInfo = null;
        }
        if (pebbleFirmwareVersionInfo != null) {
            pebbleFwVersion = pebbleFirmwareVersionInfo.getMajor()
                    + "." + pebbleFirmwareVersionInfo.getMinor()
                    + "." + pebbleFirmwareVersionInfo.getPoint()
                    + "-" + pebbleFirmwareVersionInfo.getTag();
        }
        Log.d(TAG, "pebbleFwVersion:" + pebbleFwVersion);

        return pebbleFwVersion;
    }
}
