package com.njackson.events.ActivityRecognitionCommand;

import com.google.android.gms.location.ActivityRecognitionResult;

/**
 * Created by server on 21/03/2014.
 */
public class NewActivityEvent {

    private ActivityRecognitionResult _activity;
    public ActivityRecognitionResult getActivity() { return _activity; }

    public NewActivityEvent(ActivityRecognitionResult activity) {
        _activity = activity;
    }

}
