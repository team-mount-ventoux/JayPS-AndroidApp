package com.njackson;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: njackson
 * Date: 22/05/2013
 * Time: 17:26
 * To change this template use File | Settings | File Templates.
 */

public class Constants {
    static final UUID WATCH_UUID = java.util.UUID.fromString("5dd35873-3bb6-44d6-8255-0e61bc3b97f5");
    
    
    static final int VERSION_ANDROID = 15;
    
    static final int LIVE_TRACKING_FRIENDS = 0x10;
    static final int ALTITUDE_DATA = 0x13;
    static final int STATE_CHANGED = 0x14;
    static final int MSG_VERSION_PEBBLE = 0x15;
    static final int MSG_VERSION_ANDROID = 0x16;
    static final int MSG_LIVE_SHORT = 0x17;
    //static final int MSG_LIVE_ASK_NAMES = 0x18;
    static final int MSG_LIVE_NAME0 = 0x19;
    static final int MSG_LIVE_NAME1 = 0x20;
    static final int MSG_LIVE_NAME2 = 0x21;
    static final int MSG_LIVE_NAME3 = 0x22;
    static final int MSG_LIVE_NAME4 = 0x23;    

    static final String PREFS_NAME = "PebbleBikePrefs";

    static final int PLAY_PRESS = 0x0;
    static final int STOP_PRESS = 0x1;
    static final int REFRESH_PRESS = 0x2;
    static final int CMD_BUTTON_PRESS = 0x4;
    static final int ORUXMAPS_START_RECORD_CONTINUE_PRESS = 0x5;
    static final int ORUXMAPS_STOP_RECORD_PRESS = 0x6;
    static final int ORUXMAPS_NEW_WAYPOINT_PRESS = 0x7;
    
    static final int STATE_STOP = 0x0;
    static final int STATE_START = 0x1;

    static final int IMPERIAL = 0x0;
    static final int METRIC = 0x1;

    static final double MS_TO_KPH = 3.6;
    static final double MS_TO_MPH = 2.23693629;
    static final double M_TO_KM = 0.001;
    static final double M_TO_MILES = 0.000621371192;
    static final double M_TO_M = 1;
    static final double M_TO_FEET = 3.2808399;
}

