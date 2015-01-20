package com.njackson.events.status;

/**
 * Created by server on 21/03/2014.
 */
public class PebbleStatus {

    public enum State {
        STARTED,
        STOPPED,
        DISABLED
    }

    public State _state;
    public State getState() { return _state; }

    public PebbleStatus(State state) {
        this._state = state;
    }

}
