package com.njackson.adapters;

import com.njackson.Constants;
import com.njackson.events.GPSService.NewLocation;

import fr.jayps.android.AdvancedLocation;

/**
 * Created by njackson on 18/01/15.
 */
public class AdvancedLocationToNewLocation extends NewLocation {

    private static float _speedConversion;
    private static float _distanceConversion;
    private static float _altitudeConversion;

    public AdvancedLocationToNewLocation(AdvancedLocation advancedLocation, double xpos, double ypos, int units) {
        createUnits(units);

        this.setUnits(units);
        this.setSpeed(advancedLocation.getSpeed() * _speedConversion);
        this.setDistance(advancedLocation.getDistance()  * _distanceConversion);
        this.setAvgSpeed(advancedLocation.getAverageSpeed() * _speedConversion);
        this.setLatitude(advancedLocation.getLatitude());
        this.setLongitude(advancedLocation.getLongitude());
        this.setAltitude(advancedLocation.getAltitude() * _altitudeConversion); // m
        this.setAscent(advancedLocation.getAscent() * _altitudeConversion); // m
        this.setAscentRate(3600f * advancedLocation.getAscentRate() * _altitudeConversion); // in m/h
        this.setSlope(100f * advancedLocation.getSlope()); // in %
        this.setAccuracy(advancedLocation.getAccuracy()); // m
        this.setTime(advancedLocation.getTime());
        this.setElapsedTimeSeconds((int) (advancedLocation.getElapsedTime() / 1000));
        this.setXpos(xpos);
        this.setYpos(ypos);
        this.setBearing(advancedLocation.getBearing());
        this.setHeartRate(255); // 255: no Heart Rate available
    }

    private void createUnits(int units) {
        if(units == Constants.IMPERIAL) {
            _speedConversion = Constants.MS_TO_MPH;
            _distanceConversion = Constants.M_TO_MILES;
            _altitudeConversion = Constants.M_TO_FEET;
        } else {
            _speedConversion = Constants.MS_TO_KPH;
            _distanceConversion = Constants.M_TO_KM;
            _altitudeConversion = Constants.M_TO_M;
        }
    }

}
