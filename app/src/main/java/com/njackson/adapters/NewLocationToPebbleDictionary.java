package com.njackson.adapters;

import com.getpebble.android.kit.util.PebbleDictionary;
import com.njackson.Constants;
import com.njackson.events.GPSServiceCommand.NewLocation;

/**
 * Created by server on 25/03/2014.
 * Converts NewLocationEvent to a Pebble Dictionary object
 *
 * This code needs to be refactored as there is dependency on objects other than
 * Location however this dependency extends into the native code running on the
 * watch face that this needs to be changed at the same time
 */
public class NewLocationToPebbleDictionary extends PebbleDictionary{

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

    public NewLocationToPebbleDictionary(NewLocation event, boolean serviceRunning, boolean debug, boolean liveTrackingEnabled, int refreshInterval, int heartRate, int cadence) {
        // todo(jay) remove param heartRate

        PebbleDictionary dic = new PebbleDictionary();
        byte[] data = new byte[23];

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
        if (cadence < 255) {
            // CSC sensor is configured and cadence is received, sent it instead of hr (both are not supported yet at the same time)
            data[BYTE_HEARTRATE] = (byte) (cadence % 256);
        } else {
            data[BYTE_HEARTRATE] = (byte) (heartRate % 256);
        }

        data[BYTE_MAXSPEED1] = (byte) (((int) (Math.floor(10 * event.getMaxSpeed()) / 1)) % 256);
        data[BYTE_MAXSPEED2] = (byte) (((int) (Math.floor(10 * event.getMaxSpeed()) / 1)) / 256);

        this.addBytes(Constants.PEBBLE_LOCATION_DATA_V2, data);
    }
}
