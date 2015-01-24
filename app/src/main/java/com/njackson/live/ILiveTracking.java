package com.njackson.live;

import android.location.Location;

/**
 * Created by njackson on 24/01/15.
 */
public interface ILiveTracking {
    public void setLogin(String login);
    public void setPassword(String password);
    public void setUrl(String url);
    public boolean addPoint(Location firstLocation, Location location, double altitude, int heart_rate);
}
