package com.android.AdventureTracker;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import com.google.android.gms.location.LocationListener;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;

/**
 * Created with IntelliJ IDEA.
 * User: njackson
 * Date: 23/05/2013
 * Time: 13:30
 * To change this template use File | Settings | File Templates.
 */
public class GPSService extends Service implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {

    private int _updates;
    private LocationClient _locationClient;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleCommand(intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy (){
        Log.d("ActivityIntent","Stopped GPS Service");
        PebbleKit.closeAppOnPebble(getApplicationContext(), Constants.WATCH_UUID);
        _locationClient.removeLocationUpdates(this);
        _locationClient.disconnect();
    }

    private void handleCommand(Intent intent) {
        Log.d("ActivityIntent","Started GPS Service");
        PebbleKit.startAppOnPebble(getApplicationContext(), Constants.WATCH_UUID);
        _updates = 0;
        _locationClient = new LocationClient(getApplicationContext(),this,this);
        _locationClient.connect();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d("ActivityIntent","GPS CONNECTED");
        LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setInterval(500);
        //request.setFastestInterval(500);
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
        PebbleKit.sendDataToPebble(getApplicationContext(), Constants.WATCH_UUID, dic);
    }
}
