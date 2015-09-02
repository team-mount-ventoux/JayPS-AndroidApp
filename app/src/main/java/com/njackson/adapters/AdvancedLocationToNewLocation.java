package com.njackson.adapters;

import android.util.Log;

import com.njackson.Constants;
import com.njackson.events.GPSServiceCommand.NewLocation;

import fr.jayps.android.AdvancedLocation;

/**
 * Created by njackson on 18/01/15.
 */
public class AdvancedLocationToNewLocation extends NewLocation {

    private static String TAG = "PB-AdvancedLocationToNewLocation";
    private static boolean _speedInversion = false;
    private static float _speedConversion;
    private static float _distanceConversion;
    private static float _altitudeConversion;

    public AdvancedLocationToNewLocation(AdvancedLocation advancedLocation, double xpos, double ypos, int units) {
        createUnits(units);

        this.setUnits(units);
        if (_speedInversion) {
            this.setSpeed(advancedLocation.getSpeed() > 0 ? 1/(advancedLocation.getSpeed() * _speedConversion) : 0);
            this.setMaxSpeed(advancedLocation.getMaxSpeed() > 0 ? 1/(advancedLocation.getMaxSpeed() * _speedConversion) : 0);
            this.setAvgSpeed(advancedLocation.getAverageSpeed() > 0 ? 1/(advancedLocation.getAverageSpeed() * _speedConversion) : 0);
        } else {
            this.setSpeed(advancedLocation.getSpeed() * _speedConversion);
            this.setMaxSpeed(advancedLocation.getMaxSpeed() * _speedConversion);
            this.setAvgSpeed(advancedLocation.getAverageSpeed() * _speedConversion);
        }
        this.setDistance(advancedLocation.getDistance() * _distanceConversion);
        this.setLatitude(advancedLocation.getLatitude());
        this.setLongitude(advancedLocation.getLongitude());
        this.setAltitude(advancedLocation.getAltitude() * _altitudeConversion); // m
        this.setAscent(advancedLocation.getAscent() * _altitudeConversion); // m
        this.setAscentRate(3600f * advancedLocation.getAscentRate() * _altitudeConversion); // in m/h
        this.setNbAscent(advancedLocation.getNbAscent());
        this.setSlope(100f * advancedLocation.getSlope()); // in %
        this.setAccuracy(advancedLocation.getAccuracy()); // m
        this.setTime(advancedLocation.getTime());
        this.setElapsedTimeSeconds((int) (advancedLocation.getElapsedTime() / 1000));
        this.setXpos(xpos);
        this.setYpos(ypos);
        this.setBearing(advancedLocation.getBearing());
        this.setHeartRate(255); // 255: no Heart Rate available
        this.setCyclingCadence(255); // 255: no cadence available
        this.setRunningCadence(255); // 255: no cadence available
    }

    private void createUnits(int units) {
        if (units == Constants.IMPERIAL) {
            _speedInversion = false;
            _speedConversion = Constants.MS_TO_MPH;
            _distanceConversion = Constants.M_TO_MILES;
            _altitudeConversion = Constants.M_TO_FEET;
        } else if (units == Constants.METRIC) {
            _speedInversion = false;
            _speedConversion = Constants.MS_TO_KPH;
            _distanceConversion = Constants.M_TO_KM;
            _altitudeConversion = Constants.M_TO_M;
        } else if (units == Constants.NAUTICAL_IMPERIAL) {
            _speedInversion = false;
            _speedConversion = Constants.MS_TO_KNOT;
            _distanceConversion = Constants.M_TO_NM;
            _altitudeConversion = Constants.M_TO_FEET;
        } else if (units == Constants.NAUTICAL_METRIC) {
            _speedInversion = false;
            _speedConversion = Constants.MS_TO_KNOT;
            _distanceConversion = Constants.M_TO_NM;
            _altitudeConversion = Constants.M_TO_M;
        } else if (units == Constants.RUNNING_IMPERIAL) {
            _speedInversion = true;
            _speedConversion = Constants.MS_TO_MPH / 60;
            _distanceConversion = Constants.M_TO_MILES;
            _altitudeConversion = Constants.M_TO_FEET;
        } else if (units == Constants.RUNNING_METRIC) {
            _speedInversion = true;
            _speedConversion = Constants.MS_TO_KPH / 60;
            _distanceConversion = Constants.M_TO_KM;
            _altitudeConversion = Constants.M_TO_M;
        }
    }

}
