package com.njackson.oruxmaps;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OruxMaps implements IOruxMaps {

    private static final String TAG = "PB-OruxMaps";

    private final Context _context;

    public OruxMaps(Context context) {
        _context = context;
    }

    private void sendOruxIntent(String intentName, boolean donateVersion) {
        String oruxIntent = "";
        ComponentName cn;
        if (donateVersion) {
            cn = new ComponentName("com.orux.oruxmapsDonate", "com.orux.oruxmaps.actividades.TaskIntentReceiver");
            oruxIntent = "com.oruxmapsDonate." + intentName;
        } else {
            // free version
            cn = new ComponentName("com.orux.oruxmaps", "com.orux.oruxmaps.actividades.TaskIntentReceiver");
            oruxIntent = "com.oruxmaps." + intentName;
        }

        Log.d(TAG, oruxIntent);
        Intent intent = new Intent();
        intent.setComponent(cn);
        intent.setAction(oruxIntent);
        _context.sendBroadcast(intent);
    }

    @Override
    public void startRecordNewSegment() {
        sendOruxIntent("INTENT_START_RECORD_NEWSEGMENT", true);
        sendOruxIntent("INTENT_START_RECORD_NEWSEGMENT", false);
    }

    @Override
    public void startRecordNewTrack() {
        sendOruxIntent("INTENT_START_RECORD_NEWTRACK", true);
        sendOruxIntent("INTENT_START_RECORD_NEWTRACK", false);
    }

    @Override
    public void startRecordContinue() {
        sendOruxIntent("INTENT_START_RECORD_CONTINUE", true);
        sendOruxIntent("INTENT_START_RECORD_CONTINUE", false);
    }

    @Override
    public void stopRecord() {
        sendOruxIntent("INTENT_STOP_RECORD", true);
        sendOruxIntent("INTENT_STOP_RECORD", false);
    }

    @Override
    public void newWaypoint() {
        sendOruxIntent("INTENT_NEW_WAYPOINT", true);
        sendOruxIntent("INTENT_NEW_WAYPOINT", false);
    }
}
