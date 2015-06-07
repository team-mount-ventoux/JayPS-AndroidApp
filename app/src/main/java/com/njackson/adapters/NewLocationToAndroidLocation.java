package com.njackson.adapters;

import android.location.Location;

import com.njackson.events.GPSServiceCommand.NewLocation;

/**
 * Created by njackson on 24/01/15.
 */
public class NewLocationToAndroidLocation extends Location {
    public NewLocationToAndroidLocation(String provider, NewLocation location) {
        super(provider);

        this.setAccuracy(location.getAccuracy());
        this.setLatitude(location.getLatitude());
        this.setLongitude(location.getLongitude());
        this.setTime(location.getTime());
        this.setAltitude(location.getAltitude());
    }
}
