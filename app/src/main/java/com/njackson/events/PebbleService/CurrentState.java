package com.njackson.events.PebbleService;

/**
 * Created by server on 21/03/2014.
 */
public class CurrentState {

    public enum State {
        STARTED,
        STOPPED,
        DISABLED
    }

    public State _state;
    public State getState() { return _state; }

    public CurrentState(State state) {
        this._state = state;
    }

}
