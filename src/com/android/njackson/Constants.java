package com.android.njackson;

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
    static final int STATE_CHANGED = 0x00;
    static final int SPEED_TEXT = 0x01;
    static final int DISTANCE_TEXT = 0x02;
    static final int AVGSPEED_TEXT = 0x03;
    static final int MEASUREMENT_UNITS = 0x04;

    static final String PREFS_NAME = "PebbleBikePrefs";

    static final int PLAY_PRESS =0x0;
    static final int STOP_PRESS = 0x1;
    static final int REFRESH_PRESS = 0x2;

    static final int STATE_START = 0x1;
    static final int STATE_STOP = 0x2;

    static final int METRIC = 0x0;
    static final int IMPERIAL = 0x1;
}

