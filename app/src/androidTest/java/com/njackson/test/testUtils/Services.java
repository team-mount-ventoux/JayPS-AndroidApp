package com.njackson.test.testUtils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.njackson.service.MainService;

/**
 * Created by njackson on 23/12/14.
 */
public class Services {

    private static final String TAG = "PB-Services";

    public static void startServiceAndWaitForReady(Class clazz, Context context) throws Exception {
        context.startService(new Intent(context,clazz));
        boolean serviceStarted = waitForServiceToStart(context, clazz, 20000);
    }

    public static boolean waitForServiceToStart(Context context, Class<MainService> serviceClass, int timeout) throws Exception {
        int timer = 0;
        while(timer < timeout) {
            if(serviceRunning(context, serviceClass)) {
                return true;
            }

            Thread.sleep(100);
            timer += 100;
        }
        Log.d(TAG, "Timeout waiting for service: " + serviceClass.getName());
        throw new Exception("Timeout waiting for Service to Start");
    }

    public static boolean waitForServiceToStop(Class serviceClass, Context context, int timeout) throws Exception {
        int timer = 0;
        while(timer < timeout) {
            if(!serviceRunning(context,serviceClass)) {
                return true;
            }

            Thread.sleep(100);
            timer += 100;
        }

        throw new Exception("Timeout waiting for Service to Stop");
    }

    public static boolean serviceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            Log.d(TAG, "SERVICE: " + service.service.getClassName());
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.d(TAG, "SERVICE RUNNING: " + serviceClass.getName());
                return true;
            }
        }
        return false;
    }

}
