package com.njackson.utils.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.njackson.activityrecognition.ActivityRecognitionService;
import com.njackson.fit.GoogleFitService;
import com.njackson.gps.GPSService;
import com.njackson.live.LiveService;
import com.njackson.virtualpebble.PebbleService;

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
    public void startLocationServices() {
        startPebbleService();
        startGPSService();
        startLiveService();
        startGoogleFitService();
    }

    @Override
    public void stopLocationServices() {
        stopGPSService();
        stopLiveService();
        stopPebbleService();
        stopGoogleFitService();
    }

    @Override
    public void startRecognitionServices() {
        startActivityRecognitionService();
    }

    @Override
    public void stopRecognitionServices() {
        stopActivityRecognitionService();
    }

    protected void startGPSService() {
        int refreshInterval = Integer.valueOf(_sharedPreferences.getString("REFRESH_INTERVAL", "1000"));
        Intent intent = new Intent(_context, GPSService.class);
        intent.putExtra("REFRESH_INTERVAL",refreshInterval);

        _context.startService(intent);
    }

    private void stopGPSService() {
        _context.stopService(new Intent(_context, GPSService.class));
    }

    private void startPebbleService() { _context.startService(new Intent(_context, PebbleService.class)); }

    private void stopPebbleService() { _context.stopService(new Intent(_context, PebbleService.class)); }

    private void startLiveService() { _context.startService(new Intent(_context, LiveService.class)); }

    private void stopLiveService() { _context.stopService(new Intent(_context, LiveService.class)); }

    private void startActivityRecognitionService() { _context.startService(new Intent(_context, ActivityRecognitionService.class)); }

    private void stopActivityRecognitionService() { _context.stopService(new Intent(_context, ActivityRecognitionService.class)); }

    private void startGoogleFitService() { _context.startService(new Intent(_context, GoogleFitService.class)); }

    private void stopGoogleFitService() { _context.stopService(new Intent(_context, GoogleFitService.class)); }

}
