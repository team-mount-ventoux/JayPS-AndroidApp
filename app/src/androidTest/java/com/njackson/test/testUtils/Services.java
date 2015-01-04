package com.njackson.test.testUtils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by njackson on 23/12/14.
 */
public class Services {

    public static void startServiceAndWaitForReady(Class clazz, Context context) throws Exception {
        context.startService(new Intent(context,clazz));
        boolean serviceStarted = waitForServiceToStart(clazz, context, 20000);
    }

    public static boolean waitForServiceToStart(Class serviceClass, Context context, int timeout) throws Exception {
        int timer = 0;
        while(timer < timeout) {
            if(serviceRunning(serviceClass, context)) {
                return true;
            }

            Thread.sleep(100);
            timer += 100;
        }
        Log.d("MAINTEST", "Timeout waiting for service: " + serviceClass.getName());
        throw new Exception("Timeout waiting for Service to Start");
    }

    public static boolean waitForServiceToStop(Class serviceClass, Context context, int timeout) throws Exception {
        int timer = 0;
        while(timer < timeout) {
            if(!serviceRunning(serviceClass, context)) {
                return true;
            }

            Thread.sleep(100);
            timer += 100;
        }

        throw new Exception("Timeout waiting for Service to Stop");
    }

    public static boolean serviceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {

            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.d("SHIZ", "SERVICE RUNNING: " + serviceClass.getName());
                return true;
            }
        }
        return false;
    }

}
