package com.njackson.events.ActivityRecognitionCommand;

import com.njackson.events.base.BaseStatus;

/**
 * Created by server on 21/03/2014.
 */
public class ActivityRecognitionStatus extends BaseStatus {

    private boolean _playServicesAvailable = true;
    public boolean playServicesAvailable() {
        return _playServicesAvailable;
    }

    public ActivityRecognitionStatus(Status status) {
        super(status);
    }

    public ActivityRecognitionStatus(Status status, boolean playServicesAvailable) {
        super(status);
        _playServicesAvailable = playServicesAvailable;
    }
}
