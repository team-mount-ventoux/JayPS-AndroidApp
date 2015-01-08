package com.njackson.events.status;

/**
 * Created by server on 21/03/2014.
 */
public class GoogleFitStatus {

    public enum State {
        SERVICE_STARTED,
        SERVICE_STOPPED,
        SERVICE_DISABLED,
        GOOGLEFIT_CONNECTED,
        GOOGLEFIT_DISCONNECTED
    }

    public State _state;
    public State getState() { return _state; }

    public GoogleFitStatus(State state) {
        this._state = state;
    }

}
