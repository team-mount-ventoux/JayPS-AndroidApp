package com.njackson;

import java.util.UUID;

/**
 * Created by server on 28/06/2014.
 */
public class Constants {

    public static final UUID WATCH_UUID = java.util.UUID.fromString("5dd35873-3bb6-44d6-8255-0e61bc3b97f5");

    public static final int LIVE_TRACKING_FRIENDS = 0x10;
    public static final int PEBBLE_LOCTATION_DATA = 0x13;
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

    public static final int IMPERIAL = 0x0;
    public static final int METRIC = 0x1;

    public static final int REFRESH_INTERVAL_DEFAULT = 500;

    public static final double MS_TO_KPH = 3.6;
    public static final double MS_TO_MPH = 2.23693629;
    public static final double M_TO_KM = 0.001;
    public static final double M_TO_MILES = 0.000621371192;
    public static final double M_TO_M = 1;
    public static final double M_TO_FEET = 3.2808399;
}
