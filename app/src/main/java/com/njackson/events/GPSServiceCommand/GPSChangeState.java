package com.njackson.events.GPSServiceCommand;

/**
 * Created by njackson on 24/01/15.
 */
public class GPSChangeState {
    public enum State {
        START,
        STOP
    }

    public int _refreshInterval = 1000;
    public int getRefreshInterval() { return _refreshInterval; }

    public State _state;
    public State getState() { return _state; }


    public GPSChangeState(State state) {
        this._state = state;
    }
    public GPSChangeState(State state, int refreshInterval) {
        this._refreshInterval = refreshInterval;
        this._state = state;
    }
}
