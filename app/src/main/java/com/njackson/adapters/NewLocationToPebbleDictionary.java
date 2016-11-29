package com.njackson.adapters;

import android.location.Location;
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
    public static final short NAV_BYTE_NB_PAGES = 6;
    public static final short NAV_BYTE_PAGE_NUMBER = 7;
    public static final short NAV_BYTE_NEXT_INDEX1 = 8;
    public static final short NAV_BYTE_NEXT_INDEX2 = 9;
    public static final short NAV_BYTE_SETTINGS = NAV_BYTE_NEXT_INDEX2;
    public static final short NAV_BYTES_POINTS = 10;

    public static final short NAV_POS_NOTIFICATION  = 7; // 1 bit

    public static final short NAV_NB_POINTS = 20;
    public static final short NAV_NB_BYTES = NAV_BYTES_POINTS + 4 * NAV_NB_POINTS;
    public static final short NB_POINTS_PER_PAGE = 5;

    private Location _firstLocation = null;

    public NewLocationToPebbleDictionary(NewLocation event, Navigator navigator, boolean serviceRunning, boolean debug, boolean liveTrackingEnabled, int refreshInterval, int watchfaceVersion, boolean nav_notification) {

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

        putDataUInt8(data, BYTE_ACCURACY, (int) Math.ceil(event.getAccuracy()));
        putDataUInt16(data, BYTE_DISTANCE1, (int) (Math.floor(100 * event.getDistance())));
        putDataUInt16(data, BYTE_TIME1, event.getElapsedTimeSeconds());
        putDataUInt16(data, BYTE_ALTITUDE1, (int) event.getAltitude());
        putDataInt16(data, BYTE_ASCENT1, (int) event.getAscent());
        putDataInt16(data, BYTE_ASCENTRATE1, (int) event.getAscentRate());
        putDataInt8(data, BYTE_SLOPE, (int) (event.getSlope()));
        putDataInt16(data, BYTE_XPOS1, (int) event.getXpos());
        putDataInt16(data, BYTE_YPOS1, (int) event.getYpos());
        putDataUInt16(data, BYTE_SPEED1, (int) Math.floor(10 * event.getSpeed()));
        putDataUInt8(data, BYTE_BEARING, (int)  (event.getBearing() / 360 * 256));

        putDataUInt8(data, BYTE_HEARTRATE, event.getHeartRate());
        if (location_data_version >= Constants.PEBBLE_LOCATION_DATA_V3) {
            putDataUInt8(data, BYTE_CADENCE, event.getCyclingCadence());
        } else {
            // old protocol, only one field (BYTE_HEARTRATE) for both hr and cadence
            if (event.getCyclingCadence() < 255) {
                // CSC sensor is configured and cadence is received, sent it instead of hr (both are not supported yet at the same time)
                putDataUInt8(data, BYTE_HEARTRATE, event.getCyclingCadence());
            }
        }

        putDataUInt16(data, BYTE_MAXSPEED1, (int) (Math.floor(10 * event.getMaxSpeed())));

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
            putDataUInt8(data_heartmax, 0, event.getHeartRateMax());
            putDataUInt8(data_heartmax, 1, event.getHeartRateMode());
            this.addBytes(Constants.MSG_HR_MAX, data_heartmax);
        }
        if (event.getSendNavigation()) {
            byte[] data_navigation = new byte[NAV_NB_BYTES];

            // in m, 0-65.535km
            putDataUInt16(data_navigation, NAV_BYTE_DISTANCE1, (int) navigator.getNextDistance(event.getUnits()));

            // in 0.01km, 0-655km
            putDataUInt16(data_navigation, NAV_BYTE_DTD1, (int) (Math.floor(navigator.getDistanceToDestination(event.getUnits()) * 100) / 1));

            putDataUInt8(data_navigation, NAV_BYTE_BEARING, (int) (navigator.getNextBearing() / 360 * 256));

            // in 10m, 0-2.56km
            putDataUInt8(data_navigation, NAV_BYTE_ERROR, (int) (Math.floor(Math.abs(navigator.getError()) / 10)));

            int curPageNumber = (int) Math.floor(navigator.getNextIndex() / NB_POINTS_PER_PAGE);
            int firstPageNumberSent = Math.max(0, (curPageNumber - 1));
            int firstIndex = firstPageNumberSent * NB_POINTS_PER_PAGE;
            //Log.d(TAG, "nextIndex:" + navigator.getNextIndex() + " curPageNumber:" + curPageNumber + " firstPageNumberSent:" + firstPageNumberSent + " firstIndex:" + firstIndex);

            // 0-256 pages (=> 5*256=1280 points)
            putDataUInt8(data_navigation, NAV_BYTE_NB_PAGES, (int) Math.ceil(navigator.getNbPoints() / NB_POINTS_PER_PAGE));
            putDataUInt8(data_navigation, NAV_BYTE_PAGE_NUMBER, firstPageNumberSent);

            putDataUInt16(data_navigation, NAV_BYTE_NEXT_INDEX1, navigator.getNextIndex());

            data_navigation[NAV_BYTE_SETTINGS] += (byte) ((nav_notification ? 1: 0) * (1<<NAV_POS_NOTIFICATION));

            if (_firstLocation == null) {
                _firstLocation = event.getFirstLocation();
            }
            double xpos, ypos;
            for (int i = 0; i < NAV_NB_POINTS; i++) {
                xpos = ypos = 0xFFFF;
                Location point = navigator.getPoint(firstIndex + i);

                if (point != null && _firstLocation != null) {
                    xpos = _firstLocation.distanceTo(point) * Math.sin(_firstLocation.bearingTo(point) / 180 * 3.1415);
                    xpos = Math.floor(xpos / 10);
                    ypos = _firstLocation.distanceTo(point) * Math.cos(_firstLocation.bearingTo(point) / 180 * 3.1415);
                    ypos = Math.floor(ypos / 10);
                }
                putDataInt16(data_navigation, NAV_BYTES_POINTS + 4 * i, (int) xpos);
                putDataInt16(data_navigation, NAV_BYTES_POINTS + 2 + 4 * i, (int) ypos);
                //Log.d(TAG, i + " xpos:" + xpos + " ypos:" + ypos);
            }

            this.addBytes(Constants.MSG_NAVIGATION, data_navigation);
        }
    }

    static void putDataInt16(byte[] data, int index, int value) {
        data[index] = (byte) Math.abs(value % 256);
        data[index + 1] = (byte) ((Math.abs(value) / 256) % 128);
        if (value < 0) {
            data[index + 1] += 128;
        }
    }
    static void putDataUInt16(byte[] data, int index, int value) {
        data[index] = (byte) (value % 256);
        data[index + 1] = (byte) (value / 256);
    }
    static void putDataInt8(byte[] data, int index, int value) {
        data[index] = (byte) Math.abs(value % 128);
        if (value < 0) {
            data[index + 1] += 128;
        }
    }
    static void putDataUInt8(byte[] data, int index, int value) {
        data[index] = (byte) (value % 256);
    }
}
