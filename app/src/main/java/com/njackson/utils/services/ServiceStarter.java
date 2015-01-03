package com.njackson.utils.services;

import android.content.Context;
import android.content.Intent;

import com.njackson.activityrecognition.ActivityRecognitionService;
import com.njackson.gps.GPSService;
import com.njackson.live.LiveService;
import com.njackson.virtualpebble.PebbleService;

/**
 * Created by njackson on 03/01/15.
 */
public class ServiceStarter implements IServiceStarter {

    Context _context;

    public ServiceStarter(Context context) {
        _context = context;
    }

    @Override
    public void startLocationServices() {
        startPebbleService();
        startGPSService();
        startLiveService();
    }

    @Override
    public void stopLocationServices() {
        stopGPSService();
        stopLiveService();
        stopPebbleService();
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
        _context.startService(new Intent(_context, GPSService.class));
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

}
