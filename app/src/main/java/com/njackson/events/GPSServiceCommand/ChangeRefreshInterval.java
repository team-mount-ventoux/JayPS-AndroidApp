package com.njackson.events.GPSServiceCommand;

/**
 * Created by server on 21/03/2014.
 */
public class ChangeRefreshInterval {

    private int _refreshInterval;
    public int getRefreshInterval() {
        return _refreshInterval;
    }

    public ChangeRefreshInterval(int refreshinterval) {
        _refreshInterval = refreshinterval;
    }

}
