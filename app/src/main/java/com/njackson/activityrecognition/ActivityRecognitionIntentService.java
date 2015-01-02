package com.njackson.activityrecognition;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.njackson.application.PebbleBikeApplication;
import com.njackson.events.ActivityRecognitionService.NewActivityEvent;
import com.squareup.otto.Bus;

import javax.inject.Inject;

/**
 * Created with IntelliJ IDEA.
 * User: server
 * Date: 19/05/2013
 * Time: 21:38
 * To change this template use File | Settings | File Templates.
 */
public class ActivityRecognitionIntentService extends IntentService {

    private static final String TAG = "PB-ActivityRecognitionIntentService";
    @Inject Bus _bus;

    public ActivityRecognitionIntentService() {
        super("ActivityRecognitionIntentService");
        Log.d(TAG, "Start");
    }

    public ActivityRecognitionIntentService(String name) {
        super(name);
        Log.d(TAG, "Start: " + name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ((PebbleBikeApplication)getApplication()).inject(this);

        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

            Log.d(TAG, "Handle Intent");

            switch(result.getMostProbableActivity().getType()) {

                case DetectedActivity.ON_BICYCLE:
                    Log.d(TAG, "ON_BICYCLE");
                    sendReply(result.getMostProbableActivity().getType());
                    break;
                case DetectedActivity.ON_FOOT:
                    Log.d(TAG, "ON_FOOT");
                    sendReply(result.getMostProbableActivity().getType());
                    break;
                case DetectedActivity.TILTING:
                    Log.d(TAG, "TILTING");
                    break;
                case DetectedActivity.STILL:
                    Log.d(TAG, "STILL");
            }

        }
    }

    private void sendReply(int type) {
        _bus.post(new NewActivityEvent(type));
    }

}