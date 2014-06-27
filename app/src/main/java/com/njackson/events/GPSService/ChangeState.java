package com.njackson.events.GPSService;

/**
 * Created by server on 27/06/2014.
 */
public class ChangeState {

    public enum Command {
        START,
        STOP,
        RESET
    }

    public Command _state;
    public Command getState() { return _state; }

    public ChangeState(Command state) {
        this._state = state;
    }

}
