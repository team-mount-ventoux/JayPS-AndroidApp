package com.njackson.activityrecognition;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.njackson.application.PebbleBikeApplication;
import com.njackson.events.ActivityRecognitionCommand.NewActivityEvent;
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

    private static final String TAG = "PB-ActivityRecoIntServ";
    @Inject Bus _bus;

    public ActivityRecognitionIntentService() {
        super("ActivityRecognitionIntentService");
    }

    public ActivityRecognitionIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ((PebbleBikeApplication)getApplication()).inject(this);

        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            logActivity(result);
            _bus.post(new NewActivityEvent(result));
        }
    }

    private void logActivity(ActivityRecognitionResult result) {
        switch (result.getMostProbableActivity().getType()) {
            case DetectedActivity.IN_VEHICLE:
                Log.d(TAG, result.getMostProbableActivity().getType() + "_IN_VEHICLE");
                break;
            case DetectedActivity.ON_BICYCLE:
                Log.d(TAG, result.getMostProbableActivity().getType() + "_ON_BICYCLE");
                break;
            case DetectedActivity.ON_FOOT:
                Log.d(TAG, result.getMostProbableActivity().getType() + "_ON_FOOT");
                break;
            case DetectedActivity.STILL:
                Log.d(TAG, result.getMostProbableActivity().getType() + "_STILL");
                break;
            case DetectedActivity.UNKNOWN:
                Log.d(TAG, result.getMostProbableActivity().getType() + "_UNKNOWN");
                break;
            case DetectedActivity.TILTING:
                Log.d(TAG, result.getMostProbableActivity().getType() + "_TILTING");
                break;
            case DetectedActivity.WALKING:
                Log.d(TAG, result.getMostProbableActivity().getType() + "_WALKING");
                break;
            case DetectedActivity.RUNNING:
                Log.d(TAG, result.getMostProbableActivity().getType() + "_RUNNING");
                break;
            default:
                Log.d(TAG, result.getMostProbableActivity().getType() + "_UNKNOWN2");
        }
        Log.d(TAG, "Probable Activities");
        for (DetectedActivity activity : result.getProbableActivities()) {
            Log.d(TAG, "Most Probable list: " + activity.toString());
        }
        Log.d(TAG, "Most Probable: " + result.getMostProbableActivity().toString());
    }
}