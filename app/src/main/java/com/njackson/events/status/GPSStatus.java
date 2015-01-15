package com.njackson.events.status;

/**
 * Created by server on 21/03/2014.
 */
public class GPSStatus {

    public enum State {
        STARTED,
        STOPPED,
        DISABLED
    }

    public State _state;
    public State getState() { return _state; }

    public GPSStatus(State state) {
        this._state = state;
    }

}
