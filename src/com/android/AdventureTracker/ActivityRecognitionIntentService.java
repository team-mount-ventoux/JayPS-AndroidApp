package com.android.AdventureTracker;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.*;


import java.util.UUID;

import static com.google.android.gms.location.DetectedActivity.ON_BICYCLE;

/**
 * Created with IntelliJ IDEA.
 * User: server
 * Date: 19/05/2013
 * Time: 21:38
 * To change this template use File | Settings | File Templates.
 */
public class ActivityRecognitionIntentService extends IntentService implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {

    static final UUID WATCH_UUID = UUID.fromString("5dd35873-3bb6-44d6-8255-0e61bc3b97f5");
    static final int SPEED_TEXT = 0;
    private static boolean _watchShown;
    private static boolean _gpsRunning;
    private static LocationClient _locationClient;

    public ActivityRecognitionIntentService() {
        super("ActivityRecognitionIntentService");
    }

    public ActivityRecognitionIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

            Log.d("ActivityIntent","Handle Intent");

            switch(result.getMostProbableActivity().getType()) {

                case DetectedActivity.ON_BICYCLE:
                    //TODO: start pebble watch face
                    // start the watch face
                    showPebbleWatchFace();
                    startGPS();
                    break;
                case DetectedActivity.TILTING:
                    break;
                default:
                    hidePebbleWatchFace();
                    stopGps();
            }

        }
    }

    private void stopGps() {
        //To change body of created methods use File | Settings | File Templates.
        if(_locationClient != null) {
            _locationClient.unregisterConnectionCallbacks(this);
            _locationClient.disconnect();
            _gpsRunning = false;
        }
    }

    private void startGPS() {
       if(!_gpsRunning) {
            _locationClient = new LocationClient(getApplicationContext(),this,this);
            _locationClient.connect();
           _gpsRunning = true;
       }
    }

    private void hidePebbleWatchFace() {
        PebbleKit.closeAppOnPebble(getApplicationContext(),WATCH_UUID);
        _watchShown = false;
    }

    private void showPebbleWatchFace() {
        if(!_watchShown) {
            PebbleKit.startAppOnPebble(getApplicationContext(),WATCH_UUID);
            _watchShown = true;
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Intent intent = new Intent(getApplicationContext(),LocationUpdateIntentService.class);
        PendingIntent callback = PendingIntent.getService(getApplicationContext(),0,intent,PendingIntent.FLAG_UPDATE_CURRENT);

        LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        _locationClient.requestLocationUpdates(request,this);
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
        Log.d("ActivityIntent","Got Speed: " + location.getSpeed());

        PebbleDictionary dic = new PebbleDictionary();
        dic.addString(SPEED_TEXT,String.valueOf(location.getSpeed()));
        PebbleKit.sendDataToPebble(getApplicationContext(),WATCH_UUID,dic);
    }
}
