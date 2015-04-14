package com.njackson.adapters;

import com.njackson.events.GPSServiceCommand.NewLocation;
import com.njackson.events.GPSServiceCommand.SavedLocation;

public class NewLocationToSavedLocation extends SavedLocation {
    public NewLocationToSavedLocation(NewLocation newLocation) {
        this.setSpeed(newLocation.getSpeed());
        this.setMaxSpeed(newLocation.getMaxSpeed());
        this.setDistance(newLocation.getDistance());
        this.setAvgSpeed(newLocation.getAverageSpeed());
        this.setLatitude(newLocation.getLatitude());
        this.setLongitude(newLocation.getLongitude());
        this.setAltitude(newLocation.getAltitude());
        this.setAscent(newLocation.getAscent());
        this.setAscentRate(newLocation.getAscentRate());
        this.setNbAscent(newLocation.getNbAscent());
        this.setSlope(newLocation.getSlope());
        this.setAccuracy(newLocation.getAccuracy());
        this.setElapsedTimeSeconds(newLocation.getElapsedTimeSeconds());
        this.setXpos(newLocation.getXpos());
        this.setYpos(newLocation.getYpos());
        this.setBearing(newLocation.getBearing());
        this.setUnits(newLocation.getUnits());
        this.setTime(newLocation.getTime());
        this.setHeartRate(newLocation.getHeartRate());
    }
}
