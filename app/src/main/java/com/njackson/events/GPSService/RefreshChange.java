package com.njackson.events.GPSService;

/**
 * Created by server on 21/03/2014.
 */
public class RefreshChange {

    private int _refreshInterval;
    public int getRefreshInterval() {
        return _refreshInterval;
    }

    public RefreshChange(int refreshinterval) {
        _refreshInterval = refreshinterval;
    }

}
