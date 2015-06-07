package com.njackson.live;

import android.location.Location;

import com.squareup.otto.Bus;

/**
 * Created by njackson on 24/01/15.
 */
public interface ILiveTracking {
    public void setLogin(String login);
    public void setPassword(String password);
    public void setUrl(String url);
    public void setBus(Bus bus);
    public boolean addPoint(Location firstLocation, Location location, int heart_rate, int cadence);
}
