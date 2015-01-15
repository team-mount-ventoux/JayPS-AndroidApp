package com.njackson.utils.googleplay;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

/**
 * Created by njackson on 11/01/15.
 */
public interface IGoogleFitSessionManager {
    public List<GoogleFitSessionManager.SessionData> getSessionData();
    public List<DataPoint> getDataPoints();

    public void startSession(long startTime, GoogleApiClient client);
    public void addDataPoint(long startTime, int activity);
    public void saveActiveSession(long endTime);
}
