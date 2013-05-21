package com.android.AdventureTracker;

import android.app.IntentService;
import android.content.Intent;
import com.getpebble.android.kit.PebbleKit;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;


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

    static final UUID WATCH_UUID = UUID.fromString("5dd35873-3bb6-44d6-8255-0e61bc3b97f5");

    public ActivityRecognitionIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

            // start the watch face
            showPebbleWatchFace();

            switch(result.getMostProbableActivity().getType()) {

                case DetectedActivity.ON_BICYCLE:
                    //TODO: start pebble watch face
                    break;
                case DetectedActivity.TILTING:
                    break;
                default:
                    //TODO: stop pebble watch face

            }

        }
    }

    private void showPebbleWatchFace() {

        PebbleKit.startAppOnPebble(getApplicationContext(),WATCH_UUID);

    }
}
