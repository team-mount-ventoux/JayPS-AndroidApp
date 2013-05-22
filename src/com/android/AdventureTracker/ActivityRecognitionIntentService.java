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
public class ActivityRecognitionIntentService extends IntentService {

    private static boolean _watchShown;

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
            //GPSUtils.getInstance(getApplicationContext()).startGPS();
            Log.d("ActivityIntent","Handle Intent");

            switch(result.getMostProbableActivity().getType()) {

                case DetectedActivity.ON_BICYCLE:
                    //TODO: start pebble watch face
                    // start the watch face
                    Log.d("ActivityIntent","ON_BICYCLE");
                    showPebbleWatchFace();
                    GPSUtils.getInstance(getApplicationContext()).startGPS();
                    break;
                case DetectedActivity.TILTING:
                    Log.d("ActivityIntent","TILTING");
                    break;
                case DetectedActivity.STILL:
                    Log.d("ActivityIntent","STILL");
                default:
                    hidePebbleWatchFace();
                    GPSUtils.getInstance(getApplicationContext()).stopGps();
            }

        }
    }

    private void hidePebbleWatchFace() {
        PebbleKit.closeAppOnPebble(getApplicationContext(),Constants.WATCH_UUID);
        _watchShown = false;
    }

    private void showPebbleWatchFace() {
        if(!_watchShown) {
            PebbleKit.startAppOnPebble(getApplicationContext(),Constants.WATCH_UUID);
            _watchShown = true;
        }
    }
}
