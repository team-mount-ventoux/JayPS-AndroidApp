package com.njackson.events.status;

import com.google.android.gms.common.ConnectionResult;

/**
 * Created by server on 21/03/2014.
 */
public class GoogleFitStatus {

    public enum State {
        SERVICE_STARTED,
        SERVICE_STOPPED,
        SERVICE_DISABLED,
        GOOGLEFIT_CONNECTED,
        GOOGLEFIT_CONNECTION_FAILED,
        GOOGLEFIT_DISCONNECTED
    }

    private State _state;
    public State getState() { return _state; }

    private ConnectionResult _connectionResult = null;
    public ConnectionResult getConnectionResult() { return _connectionResult; }

    public GoogleFitStatus(State state) {
        this._state = state;
    }

    public GoogleFitStatus(State state, ConnectionResult connectionResult) {
        _state = state;
        _connectionResult = connectionResult;
    }

}
