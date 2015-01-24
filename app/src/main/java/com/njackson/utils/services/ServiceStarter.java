package com.njackson.utils.services;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.njackson.activityrecognition.ActivityRecognitionService;
import com.njackson.fit.GoogleFitService;
import com.njackson.gps.GPSServiceCommand;
import com.njackson.live.LiveService;
import com.njackson.oruxmaps.OruxMapsService;
import com.njackson.pebble.PebbleServiceCommand;

/**
 * Created by njackson on 03/01/15.
 */
public class ServiceStarter implements IServiceStarter {

    Context _context;
    SharedPreferences _sharedPreferences;

    public ServiceStarter(Context context, SharedPreferences preferences) {
        _context = context;
        _sharedPreferences = preferences;
    }

    @Override
    public void startPebbleServices() {
        _context.startService(new Intent(_context, PebbleServiceCommand.class));
    }

    @Override
    public void stopPebbleServices() {
        _context.stopService(new Intent(_context, PebbleServiceCommand.class));
    }

    @Override
    public void startActivityServices() {
        _context.startService(new Intent(_context, ActivityRecognitionService.class));
    }

    @Override
    public void stopActivityServices() {
        _context.stopService(new Intent(_context, ActivityRecognitionService.class));
    }

    @Override
    public void startLocationServices() {
        startGPSService();
        startLiveService();
        startGoogleFitService();
        startOruxService();
    }

    @Override
    public void stopLocationServices() {
        stopGPSService();
        stopLiveService();
        stopGoogleFitService();
        stopOruxService();
    }

    @Override
    public boolean serviceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) _context.getSystemService(_context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    protected void startGPSService() {
        int refreshInterval = Integer.valueOf(_sharedPreferences.getString("REFRESH_INTERVAL", "1000"));
        Intent intent = new Intent(_context, GPSServiceCommand.class);
        intent.putExtra("REFRESH_INTERVAL",refreshInterval);

        _context.startService(intent);
    }

    private void stopGPSService() {
        _context.stopService(new Intent(_context, GPSServiceCommand.class));
    }

    private void startLiveService() { _context.startService(new Intent(_context, LiveService.class)); }

    private void stopLiveService() { _context.stopService(new Intent(_context, LiveService.class)); }

    private void startGoogleFitService() { _context.startService(new Intent(_context, GoogleFitService.class)); }

    private void stopGoogleFitService() { _context.stopService(new Intent(_context, GoogleFitService.class)); }

    private void startOruxService() { _context.startService(new Intent(_context, OruxMapsService.class)); }

    private void stopOruxService() { _context.stopService(new Intent(_context, OruxMapsService.class)); }

}
