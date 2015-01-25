package com.njackson.events.base;

/**
 * Created by njackson on 24/01/15.
 */
public class BaseChangeState {
    public enum State {
        START,
        STOP,
        ANNOUNCE_STATE
    }

    public State _state;
    public State getState() { return _state; }

    public BaseChangeState(State state) {
        this._state = state;
    }
}
