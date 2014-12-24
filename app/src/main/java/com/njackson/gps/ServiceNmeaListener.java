package com.njackson.gps;

import android.location.GpsStatus;
import android.location.LocationManager;

import fr.jayps.android.AdvancedLocation;

/**
 * Created by njackson on 24/12/14.
 */
public class ServiceNmeaListener implements GpsStatus.NmeaListener {
    AdvancedLocation _advancedLocation;
    LocationManager _locationManager;

    public ServiceNmeaListener(AdvancedLocation _advancedLocation) {
        this._advancedLocation = _advancedLocation;
    }

    @Override
    public void onNmeaReceived(long timestamp, String nmea) {
        if (nmea.startsWith("$GPGGA")) {

            String[] strValues = nmea.split(",");

            try {
                // Height of geoid above WGS84 ellipsoid
                double geoid_height = Double.parseDouble(strValues[11]);
                _advancedLocation.setGeoidHeight(geoid_height);
            } catch (Exception e) {
            }
        }
    }
}
