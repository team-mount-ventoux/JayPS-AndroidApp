package com.njackson.utils.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.njackson.activityrecognition.ActivityRecognitionService;
import com.njackson.fit.GoogleFitService;
import com.njackson.gps.GPSService;
import com.njackson.live.LiveService;
import com.njackson.oruxmaps.OruxMapsService;
import com.njackson.pebble.PebbleService;

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
    public void startEssentialServices() {
        startActivityRecognitionService();
        startPebbleService();
    }

    @Override
    public void stopEssentialServices() {
        stopActivityRecognitionService();
        stopPebbleService();
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

    protected void startGPSService() {
        int refreshInterval = Integer.valueOf(_sharedPreferences.getString("REFRESH_INTERVAL", "1000"));
        Intent intent = new Intent(_context, GPSService.class);
        intent.putExtra("REFRESH_INTERVAL",refreshInterval);

        _context.startService(intent);
    }

    private void stopGPSService() {
        _context.stopService(new Intent(_context, GPSService.class));
    }

    public void startPebbleService() { _context.startService(new Intent(_context, PebbleService.class)); }

    public void stopPebbleService() { _context.stopService(new Intent(_context, PebbleService.class)); }

    private void startLiveService() { _context.startService(new Intent(_context, LiveService.class)); }

    private void stopLiveService() { _context.stopService(new Intent(_context, LiveService.class)); }

    private void startActivityRecognitionService() { _context.startService(new Intent(_context, ActivityRecognitionService.class)); }

    private void stopActivityRecognitionService() { _context.stopService(new Intent(_context, ActivityRecognitionService.class)); }

    private void startGoogleFitService() { _context.startService(new Intent(_context, GoogleFitService.class)); }

    private void stopGoogleFitService() { _context.stopService(new Intent(_context, GoogleFitService.class)); }

    private void startOruxService() { _context.startService(new Intent(_context, OruxMapsService.class)); }

    private void stopOruxService() { _context.stopService(new Intent(_context, OruxMapsService.class)); }

}
