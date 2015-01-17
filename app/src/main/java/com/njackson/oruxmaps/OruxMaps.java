package com.njackson.oruxmaps;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OruxMaps implements IOruxMaps {

    private final Context _context;

    public OruxMaps(Context context) {
        _context = context;
    }

    @Override
    public void startRecordNewSegment() {
        String oruxIntent = "com.oruxmaps.INTENT_START_RECORD_NEWSEGMENT";
        Intent intent = new Intent();
        intent.setAction(oruxIntent);
        _context.sendBroadcast(intent);
    }

    @Override
    public void startRecordNewTrack() {
        String oruxIntent = "com.oruxmaps.INTENT_START_RECORD_NEWTRACK";
        Intent intent = new Intent();
        intent.setAction(oruxIntent);
        _context.sendBroadcast(intent);
    }

    @Override
    public void startRecordContinue() {
        String oruxIntent = "com.oruxmaps.INTENT_START_RECORD_CONTINUE";
        Intent intent = new Intent();
        intent.setAction(oruxIntent);
        _context.sendBroadcast(intent);
    }

    @Override
    public void stopRecord() {
        String oruxIntent = "com.oruxmaps.INTENT_STOP_RECORD";
        Intent intent = new Intent();
        intent.setAction(oruxIntent);
        _context.sendBroadcast(intent);
    }

    @Override
    public void newWaypoint() {
        String oruxIntent = "com.oruxmaps.INTENT_NEW_WAYPOINT";
        Intent intent = new Intent();
        intent.setAction(oruxIntent);
        _context.sendBroadcast(intent);
    }
}
