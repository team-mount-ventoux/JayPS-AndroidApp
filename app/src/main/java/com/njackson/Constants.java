package com.njackson;

import java.util.UUID;

/**
 * Created by server on 28/06/2014.
 */
public class Constants {

    public static final UUID WATCH_UUID = java.util.UUID.fromString("5dd35873-3bb6-44d6-8255-0e61bc3b97f5");

    public static final int LAST_VERSION_PEBBLE = 332;
    public static final int MIN_VERSION_PEBBLE = 320;

    public static final int LIVE_TRACKING_FRIENDS = 0x10;
    public static final int PEBBLE_LOCATION_DATA = 0x13;
    public static final int PEBBLE_LOCATION_DATA_V2 = 0x25;
    public static final int PEBBLE_LOCATION_DATA_V3 = 0x26;
    public static final int MIN_VERSION_PEBBLE_FOR_LOCATION_DATA_V3 = 261;
    public static final int PEBBLE_MSG_SENSOR_TEMPERATURE = 0x27;
    public static final int STATE_CHANGED = 0x14;
    public static final int MSG_VERSION_PEBBLE = 0x15;
    public static final int MSG_VERSION_ANDROID = 0x16;
    public static final int MSG_LIVE_SHORT = 0x17;

    public static final int MSG_LIVE_NAME0 = 0x19;
    public static final int MSG_LIVE_NAME1 = 0x20;
    public static final int MSG_LIVE_NAME2 = 0x21;
    public static final int MSG_LIVE_NAME3 = 0x22;
    public static final int MSG_LIVE_NAME4 = 0x23;
    public static final int MSG_BATTERY_LEVEL = 0x24;
    public static final int MSG_CONFIG = 0x28;
    public static final int MSG_HR_MAX = 0x29;
    public static final int MSG_NAVIGATION = 0x31;

    public static final int PLAY_PRESS = 0x0;
    public static final int STOP_PRESS = 0x1;
    public static final int REFRESH_PRESS = 0x2;
    public static final int CMD_BUTTON_PRESS = 0x4;
    public static final int ORUXMAPS_START_RECORD_CONTINUE_PRESS = 0x5;
    public static final int ORUXMAPS_STOP_RECORD_PRESS = 0x6;
    public static final int ORUXMAPS_NEW_WAYPOINT_PRESS = 0x7;

    public static final int STATE_STOP = 0x0;
    public static final int STATE_START = 0x1;

    public static final int IMPERIAL = 0x0;
    public static final int METRIC = 0x1;
    public static final int NAUTICAL_IMPERIAL = 0x2;
    public static final int NAUTICAL_METRIC = 0x3;
    public static final int RUNNING_IMPERIAL = 0x4;
    public static final int RUNNING_METRIC = 0x5;

    public static final int REFRESH_INTERVAL_DEFAULT = 1000;

    public static final float MS_TO_KPH = 3.6f;
    public static final float MS_TO_MPH = 2.23693629f;
    public static final float MS_TO_KNOT = 1.943844f;
    public static final float M_TO_KM = 0.001f;
    public static final float M_TO_MILES = 0.000621371192f;
    public static final float M_TO_NM = 0.000539957f;
    public static final float M_TO_M = 1f;
    public static final float M_TO_FEET = 3.2808399f;
    public static final long ACTIVITY_RECOGNITION_STILL_TIME = 30000;

    public static final String GOOGLE_FIT_SESSION_IDENTIFIER_PREFIX = "PebbleBike-";
    public static final String GOOGLE_FIT_SESSION_NAME = "Pebble Bike";

    public static final String PREFS_NAME_V1 = "PebbleBikePrefs";

    public static final int CODE_LOAD_GPX = 10;
}
