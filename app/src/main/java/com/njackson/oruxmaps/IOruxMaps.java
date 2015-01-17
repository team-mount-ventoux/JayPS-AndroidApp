package com.njackson.oruxmaps;

/**
 * Created by njackson on 17/01/15.
 */
public interface IOruxMaps {
    public void startRecordNewSegment();
    public void startRecordNewTrack();
    public void startRecordContinue();
    public void stopRecord();
    public void newWaypoint();
}
