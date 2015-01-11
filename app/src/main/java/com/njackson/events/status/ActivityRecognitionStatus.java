package com.njackson.events.status;

/**
 * Created by server on 21/03/2014.
 */
public class ActivityRecognitionStatus {

    public enum State {
        STARTED,
        STOPPED,
        PLAY_SERVICES_NOT_AVAILABLE, DISABLED
    }

    public State _state;
    public State getState() { return _state; }

    public ActivityRecognitionStatus(State state) {
        this._state = state;
    }

}
