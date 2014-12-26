package com.njackson.utils;

import com.getpebble.android.kit.util.PebbleDictionary;
import com.njackson.Constants;
import com.njackson.events.GPSService.NewLocation;

/**
 * Created by server on 25/03/2014.
 * Converts NewLocationEvent to a Pebble Dictionary object
 *
 * This code needs to be refactored as there is dependency on objects other than
 * Location however this dependency extends into the native code running on the
 * watch face that this needs to be changed at the same time
 */
public class LocationEventConverter {

    public static final short POS_UNITS=0;
    public static final short POS_SERVICE_RUNNING=1; //Refactor Out
    public static final short POS_DEBUG = 2; //Refactor Out
    public static final short POS_LIVETRACKING = 3; //Refactor Out
    public static final short POS_REFRESH = 4; //Refactor Out

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

    private static float _speedConversion;
    private static float _distanceConversion;
    private static float _altitudeConversion;

    public static PebbleDictionary convert(NewLocation event, boolean serviceRunning, boolean debug, boolean liveTrackingEnabled, int refreshInterval, int heartRate) {

        PebbleDictionary dic = new PebbleDictionary();
        byte[] data = new byte[21];

        data[BYTE_SETTINGS] = (byte) ((event.getUnits() % 2) * (1<<POS_UNITS)); // set the units
        setUnits(event.getUnits()); // set the distance units to imperial or metric


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
        data[BYTE_SETTINGS] += (byte) ((refresh_code % 4) * (1<<4)); // 2 bits

        // unused bits
        data[BYTE_SETTINGS] += (byte) (0 * (1<<6));
        data[BYTE_SETTINGS] += (byte) (0 * (1<<7));

        data[BYTE_ACCURACY] = (byte) Math.ceil(event.getAccuracy());

        data[BYTE_DISTANCE1] = (byte) (((int) (Math.floor(100 * event.getDistance() * _distanceConversion) / 1)) % 256);
        data[BYTE_DISTANCE2] = (byte) (((int) (Math.floor(100 * event.getDistance() * _distanceConversion) / 1)) / 256);

        data[BYTE_TIME1] = (byte) ((event.getElapsedTimeSeconds() / 1000) % 256);
        data[BYTE_TIME2] = (byte) ((event.getElapsedTimeSeconds() / 1000) / 256);

        data[BYTE_ALTITUDE1] = (byte) ((event.getAltitude() * _altitudeConversion) % 256);
        data[BYTE_ALTITUDE2] = (byte) ((event.getAltitude() * _altitudeConversion) / 256);

        data[BYTE_ASCENT1] = (byte) (Math.abs(event.getAscent() * _altitudeConversion) % 256);
        data[BYTE_ASCENT2] = (byte) ((Math.abs(event.getAscent() * _altitudeConversion) / 256) % 128);
        if (event.getAltitude() < 0) {
            data[BYTE_ASCENT2] += 128;
        }

        data[BYTE_ASCENTRATE1] = (byte) (Math.abs(event.getAscentRate() * _altitudeConversion) % 256);
        data[BYTE_ASCENTRATE2] = (byte) ((Math.abs(event.getAscentRate() * _altitudeConversion) / 256) % 128);
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

        data[BYTE_SPEED1] = (byte) (((int) (Math.floor(10 * event.getSpeed() * _speedConversion) / 1)) % 256);
        data[BYTE_SPEED2] = (byte) (((int) (Math.floor(10 * event.getSpeed() * _speedConversion) / 1)) / 256);
        data[BYTE_BEARING] = (byte) (((int)  (event.getBearing() / 360 * 256)) % 256);
        data[BYTE_HEARTRATE] = (byte) (heartRate % 256);


        dic.addBytes(Constants.PEBBLE_LOCTATION_DATA,data);
        return dic;

    }

    private static void setUnits(int units) {
        if(units == Constants.IMPERIAL) {
            _speedConversion = (float)Constants.MS_TO_MPH;
            _distanceConversion = (float)Constants.M_TO_MILES;
            _altitudeConversion = (float)Constants.M_TO_FEET;
        } else {
            _speedConversion = (float)Constants.MS_TO_KPH;
            _distanceConversion = (float)Constants.M_TO_KM;
            _altitudeConversion = (float)Constants.M_TO_M;
        }
    }
}
