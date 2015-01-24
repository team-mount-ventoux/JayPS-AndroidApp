package com.njackson.events.base;

/**
 * Created by njackson on 24/01/15.
 */
public class BaseStatus {
    public enum State {
        STARTED,
        STOPPED,
        DISABLED
    }

    public State _state;
    public State getState() { return _state; }

    public BaseStatus(State state) {
        this._state = state;
    }
}
