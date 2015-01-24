package com.njackson.events.ActivityRecognitionCommand;

/**
 * Created by server on 21/03/2014.
 */
public class NewActivityEvent {

    private int _activityType;
    public int getActivityType() { return _activityType; }

    public NewActivityEvent(int activityType) {
        _activityType = activityType;
    }

}
