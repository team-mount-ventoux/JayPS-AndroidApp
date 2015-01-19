package com.njackson.adapters;

import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.location.DetectedActivity;

/**
 * Created by njackson on 14/01/15.
 */
public class DetectedToFitnessActivityAdapater {

    private String _activity;

    public String getActivity() {
        return _activity;
    }

    public DetectedToFitnessActivityAdapater(int activity) {
        switch (activity) {
            case DetectedActivity.ON_BICYCLE:
                _activity = FitnessActivities.BIKING;
                break;
            case DetectedActivity.STILL:
                _activity = FitnessActivities.STILL;
                break;
            case DetectedActivity.WALKING:
                _activity = FitnessActivities.WALKING;
                break;
            case DetectedActivity.RUNNING:
                _activity = FitnessActivities.RUNNING;
                break;
            case DetectedActivity.ON_FOOT:
                _activity = FitnessActivities.ON_FOOT;
        }
    }

}
