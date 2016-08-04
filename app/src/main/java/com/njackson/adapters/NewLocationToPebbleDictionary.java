package com.njackson.adapters;

import android.util.Log;

import com.getpebble.android.kit.util.PebbleDictionary;
import com.njackson.Constants;
import com.njackson.events.GPSServiceCommand.NewLocation;
import com.njackson.gps.Navigator;

/**
 * Created by server on 25/03/2014.
 * Converts NewLocationEvent to a Pebble Dictionary object
 *
 * This code needs to be refactored as there is dependency on objects other than
 * Location however this dependency extends into the native code running on the
 * watch face that this needs to be changed at the same time
 */
public class NewLocationToPebbleDictionary extends PebbleDictionary{
    private static String TAG = "PB-NewLocationToPebbleDictionary";

    public static final short POS_UNITS           = 0; // 3 bits
    public static final short POS_SERVICE_RUNNING = 3; // 1 bit
    public static final short POS_DEBUG           = 4; // 1 bit
    public static final short POS_LIVETRACKING    = 5; // 1 bit
    public static final short POS_REFRESH         = 6; // 2 bits

    public static final short BYTE_SETTINGS = 0;
    public static final short BYTE_ACCURACY = 1;
    public static final short BYTE_DISTANCE1 = 2;
    public static final short BYTE_DISTANCE2 = 3;
    public static final short BYTE_TIME1 = 4;
    public static final short BYTE_TIME2 = 5;
    public static final short BYTE_ALTITUDE1 = 6;
    public static final short BYTE_ALTITUDE2 = 7;
    public static final short BYTE_ASCENT1 = 8;
    public static final short BYTE_ASCENT2 = 9;
    public static final short BYTE_ASCENTRATE1 = 10;
    public static final short BYTE_ASCENTRATE2 = 11;
    public static final short BYTE_SLOPE = 12;
    public static final short BYTE_XPOS1 = 13;
    public static final short BYTE_XPOS2 = 14;
    public static final short BYTE_YPOS1 = 15;
    public static final short BYTE_YPOS2 = 16;
    public static final short BYTE_SPEED1 = 17;
    public static final short BYTE_SPEED2 = 18;
    public static final short BYTE_BEARING = 19;
    public static final short BYTE_HEARTRATE = 20;
    public static final short BYTE_MAXSPEED1 = 21;
    public static final short BYTE_MAXSPEED2 = 22;
    public static final short BYTE_CADENCE = 23;

    public static final short NAV_BYTE_DISTANCE1 = 0;
    public static final short NAV_BYTE_DISTANCE2 = 1;
    public static final short NAV_BYTE_DTD1 = 2;
    public static final short NAV_BYTE_DTD2 = 3;
    public static final short NAV_BYTE_BEARING = 4;
    public static final short NAV_BYTE_ERROR = 5;
    public static final short NAV_NB_BYTES = 6;

    public NewLocationToPebbleDictionary(NewLocation event, Navigator navigator, boolean serviceRunning, boolean debug, boolean liveTrackingEnabled, int refreshInterval, int watchfaceVersion) {

        int location_data_version = Constants.PEBBLE_LOCATION_DATA_V2;
        if (watchfaceVersion >= Constants.MIN_VERSION_PEBBLE_FOR_LOCATION_DATA_V3) {
            location_data_version = Constants.PEBBLE_LOCATION_DATA_V3;
        }
        //Log.d(TAG, "watchfaceVersion=" + watchfaceVersion + " location_data_version=" + location_data_version);

        byte[] data = new byte[24];

        data[BYTE_SETTINGS] = (byte) ((event.getUnits() % 8) * (1<<POS_UNITS)); // set the units

        data[BYTE_SETTINGS] += (byte) ((serviceRunning ? 1: 0) * (1<<POS_SERVICE_RUNNING));
        data[BYTE_SETTINGS] += (byte) ((debug ? 1: 0) * (1<<POS_DEBUG));
        data[BYTE_SETTINGS] += (byte) ((liveTrackingEnabled ? 1: 0) * (1<<POS_LIVETRACKING));

        int refresh_code = 1; // 1s
        if (refreshInterval < 1000) {
            refresh_code = 0; // [0;1[
        } else if (refreshInterval >= 5000) {
            refresh_code = 3; // [5;+inf
        } else if (refreshInterval > 1000) {
            refresh_code = 2; // ]1;5[
        }
        data[BYTE_SETTINGS] += (byte) ((refresh_code % 4) * (1<<POS_REFRESH)); // 2 bits

        data[BYTE_ACCURACY] = (byte) Math.ceil(event.getAccuracy());

        data[BYTE_DISTANCE1] = (byte) (((int) (Math.floor(100 * event.getDistance()) / 1)) % 256);
        data[BYTE_DISTANCE2] = (byte) (((int) (Math.floor(100 * event.getDistance()) / 1)) / 256);

        data[BYTE_TIME1] = (byte) ((event.getElapsedTimeSeconds()) % 256);
        data[BYTE_TIME2] = (byte) ((event.getElapsedTimeSeconds()) / 256);

        data[BYTE_ALTITUDE1] = (byte) ((event.getAltitude()) % 256);
        data[BYTE_ALTITUDE2] = (byte) ((event.getAltitude()) / 256);

        data[BYTE_ASCENT1] = (byte) (Math.abs(event.getAscent()) % 256);
        data[BYTE_ASCENT2] = (byte) ((Math.abs(event.getAscent()) / 256) % 128);
        if (event.getAltitude() < 0) {
            data[BYTE_ASCENT2] += 128;
        }

        data[BYTE_ASCENTRATE1] = (byte) (Math.abs(event.getAscentRate()) % 256);
        data[BYTE_ASCENTRATE2] = (byte) ((Math.abs(event.getAscentRate()) / 256) % 128);
        if (event.getAscentRate() < 0) {
            data[BYTE_ASCENTRATE2 ] += 128;
        }

        data[BYTE_SLOPE] = (byte) ((Math.abs(event.getSlope())) % 128);
        if (event.getSlope() < 0) {
            data[BYTE_SLOPE] += 128;
        }

        data[BYTE_XPOS1] = (byte) (Math.abs(event.getXpos()) % 256);
        data[BYTE_XPOS2] = (byte) (Math.abs(event.getXpos() / 256) % 128);
        if (event.getXpos() < 0) {
            data[BYTE_XPOS2] += 128;
        }
        data[BYTE_YPOS1] = (byte) (Math.abs(event.getYpos()) % 256);
        data[BYTE_YPOS2] = (byte) ((Math.abs(event.getYpos()) / 256) % 128);
        if (event.getYpos() < 0) {
            data[BYTE_YPOS2] += 128;
        }

        data[BYTE_SPEED1] = (byte) (((int) (Math.floor(10 * event.getSpeed()) / 1)) % 256);
        data[BYTE_SPEED2] = (byte) (((int) (Math.floor(10 * event.getSpeed()) / 1)) / 256);
        data[BYTE_BEARING] = (byte) (((int)  (event.getBearing() / 360 * 256)) % 256);

        data[BYTE_HEARTRATE] = (byte) (event.getHeartRate() % 256);
        if (location_data_version >= Constants.PEBBLE_LOCATION_DATA_V3) {
            data[BYTE_CADENCE] = (byte) (event.getCyclingCadence() % 256);
        } else {
            // old protocol, only one field (BYTE_HEARTRATE) for both hr and cadence
            if (event.getCyclingCadence() < 255) {
                // CSC sensor is configured and cadence is received, sent it instead of hr (both are not supported yet at the same time)
                data[BYTE_HEARTRATE] = (byte) (event.getCyclingCadence() % 256);
            }
        }

        data[BYTE_MAXSPEED1] = (byte) (((int) (Math.floor(10 * event.getMaxSpeed()) / 1)) % 256);
        data[BYTE_MAXSPEED2] = (byte) (((int) (Math.floor(10 * event.getMaxSpeed()) / 1)) / 256);

        this.addBytes(location_data_version, data);

        if (location_data_version >= Constants.PEBBLE_LOCATION_DATA_V3 && event.getTemperature() != 0) {
            this.addInt16(Constants.PEBBLE_MSG_SENSOR_TEMPERATURE, (short) Math.floor(10 * event.getTemperature()));
            //Log.d(TAG, "temperature:" + event.getTemperature());
        }

        if (event.getBatteryLevel() != 0) {
            this.addInt32(Constants.MSG_BATTERY_LEVEL, event.getBatteryLevel());
            //Log.d(TAG, "batteryLevel:" + event.getBatteryLevel());
        }
        if (event.getHeartRateMax() != 0) {
            byte[] data_heartmax = new byte[2];
            data_heartmax[0] = (byte) (event.getHeartRateMax() % 256);
            data_heartmax[1] = (byte) (event.getHeartRateMode() % 256);
            this.addBytes(Constants.MSG_HR_MAX, data_heartmax);
        }
        if (navigator.getNbPoints() > 0) {
            byte[] data_navigation = new byte[NAV_NB_BYTES];

            // in m, 0-65.535km
            data_navigation[NAV_BYTE_DISTANCE1] = (byte) ((navigator.getNextDistance()) % 256);
            data_navigation[NAV_BYTE_DISTANCE2] = (byte) ((navigator.getNextDistance()) / 256);

            // in 10m, 0-655km
            data_navigation[NAV_BYTE_DTD1] = (byte) (((int) (Math.floor(navigator.getDistanceToDestination() / 10) / 1)) % 256);
            data_navigation[NAV_BYTE_DTD2] = (byte) (((int) (Math.floor(navigator.getDistanceToDestination() / 10) / 1)) / 256);

            data_navigation[NAV_BYTE_BEARING] = (byte) (((int)  (navigator.getNextBearing() / 360 * 256)) % 256);

            // in 10m, 0-2.56km
            data_navigation[NAV_BYTE_ERROR] = (byte) (((int) (Math.floor(Math.abs(navigator.getError()) / 10) / 1)) % 256);

            //navigator.getNextIndex()

            this.addBytes(Constants.MSG_NAVIGATION, data_navigation);
        }
    }
}
