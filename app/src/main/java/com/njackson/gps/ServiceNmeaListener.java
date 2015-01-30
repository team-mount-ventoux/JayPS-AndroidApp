package com.njackson.gps;

import android.content.SharedPreferences;
import android.location.GpsStatus;
import android.location.LocationManager;
import android.util.Log;


import com.njackson.state.IGPSDataStore;

import javax.inject.Inject;

import fr.jayps.android.AdvancedLocation;

/**
 * Created by njackson on 24/12/14.
 */
public class ServiceNmeaListener implements GpsStatus.NmeaListener {

    private static final String TAG = "PB-ServiceNmeaListener";

    private LocationManager _locationManager;
    private IGPSDataStore _dataStore;
    private AdvancedLocation _advancedLocation;

    public ServiceNmeaListener(AdvancedLocation advancedLocation, LocationManager locationmanager, IGPSDataStore dataStore) {
        _advancedLocation = advancedLocation;
        _locationManager = locationmanager;
        _dataStore = dataStore;
    }

    @Override
    public void onNmeaReceived(long timestamp, String nmea) {
        if (nmea.startsWith("$GPGGA")) {

            String[] strValues = nmea.split(",");

            try {
                // Height of geoid above WGS84 ellipsoid
                double geoid_height = Double.parseDouble(strValues[11]);

                _advancedLocation.setGeoidHeight(geoid_height);

                _dataStore.setGEOIDHeight((float) geoid_height);
                _dataStore.commit();

                // no longer need Nmea updates
                _locationManager.removeNmeaListener(this);
            } catch (Exception e) {
            }
        }
    }
}
