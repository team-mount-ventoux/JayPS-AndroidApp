package com.njackson;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OruxMaps {
	
    private static final String TAG = "PB-OruxMaps";

    public static void startRecordNewSegment(Context context) {
        String oruxIntent = "com.oruxmaps.INTENT_START_RECORD_NEWSEGMENT";
        Log.d(TAG, "Sending " + oruxIntent);
        Intent intent = new Intent();
        intent.setAction(oruxIntent);
        context.sendBroadcast(intent);    
    }
    public static void startRecordNewTrack(Context context) {
        String oruxIntent = "com.oruxmaps.INTENT_START_RECORD_NEWTRACK";
        Log.d(TAG, "Sending " + oruxIntent);
        Intent intent = new Intent();
        intent.setAction(oruxIntent);
        context.sendBroadcast(intent);    
    }
    public static void startRecordContinue(Context context) {
        String oruxIntent = "com.oruxmaps.INTENT_START_RECORD_CONTINUE";
        Log.d(TAG, "Sending " + oruxIntent);
        Intent intent = new Intent();
        intent.setAction(oruxIntent);
        context.sendBroadcast(intent);    
    }
    public static void stopRecord(Context context) {
        String oruxIntent = "com.oruxmaps.INTENT_STOP_RECORD";
        Log.d(TAG, "Sending " + oruxIntent);
        Intent intent = new Intent();
        intent.setAction(oruxIntent);
        context.sendBroadcast(intent);    
    }
    public static void newWaypoint(Context context) {
        String oruxIntent = "com.oruxmaps.INTENT_NEW_WAYPOINT";
        Log.d(TAG, "Sending " + oruxIntent);
        Intent intent = new Intent();
        intent.setAction(oruxIntent);
        context.sendBroadcast(intent);    
    }
}
