package com.android.AdventureTracker;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

/**
 * Created with IntelliJ IDEA.
 * User: njackson
 * Date: 22/05/2013
 * Time: 16:48
 * To change this template use File | Settings | File Templates.
 */
public class GPSUtils implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, LocationListener{

    private static GPSUtils _this;
    private LocationClient _locationClient;
    private Context _context;
    private boolean _gpsRunning;
    private int _updates = 0;

    public GPSUtils(Context context) {
        Log.d("ActivityIntent","INIT GPS");
        _context = context;
    }

    public static GPSUtils getInstance(Context context) {
        if(_this == null)
            _this = new GPSUtils(context);

        return _this;
    }

    public void stopGps() {
        //To change body of created methods use File | Settings | File Templates.
        if(_gpsRunning) {
            Log.d("ActivityIntent","STOP GPS");
            _locationClient.unregisterConnectionCallbacks(this);
            _locationClient.disconnect();
            _gpsRunning = false;
        }
    }

    public void startGPS() {
        if(_gpsRunning == true || _this == null)
            return;

        Log.d("ActivityIntent","START GPS");
        _updates = 0;
        _locationClient = new LocationClient(_context,this,this);
        _locationClient.connect();
        _gpsRunning = true;
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d("ActivityIntent","GPS CONNECTED");
        LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setFastestInterval(500);
        _locationClient.requestLocationUpdates(request, this);
    }

    @Override
    public void onDisconnected() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("ActivityIntent", "Got Speed: " + location.getSpeed());
        _updates++;
        PebbleDictionary dic = new PebbleDictionary();
        dic.addString(Constants.SPEED_TEXT,String.valueOf(location.getSpeed()));
        dic.addString(Constants.DISTANCE_TEXT,String.valueOf(_updates));
        dic.addString(Constants.AVGSPEED_TEXT,String.valueOf(location.getTime()));
        PebbleKit.sendDataToPebble(_context, Constants.WATCH_UUID, dic);
    }

}
