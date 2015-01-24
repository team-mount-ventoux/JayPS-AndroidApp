package com.njackson.events.GPSServiceCommand;

import com.njackson.events.base.BaseChangeState;

/**
 * Created by njackson on 24/01/15.
 */
public class GPSChangeState extends BaseChangeState{

    public int _refreshInterval = 1000;
    public int getRefreshInterval() { return _refreshInterval; }

    public GPSChangeState(State state){
        super(state);
    }

    public GPSChangeState(State state, int refreshInterval) {
        super(state);
        this._refreshInterval = refreshInterval;
        this._state = state;
    }
}
