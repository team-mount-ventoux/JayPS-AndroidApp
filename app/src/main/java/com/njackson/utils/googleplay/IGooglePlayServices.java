package com.njackson.utils.googleplay;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.IntentSender;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.SessionInsertRequest;

/**
 * Created by njackson on 02/01/15.
 */
public interface IGooglePlayServices {
    public int isGooglePlayServicesAvailable(Context context);
    public void requestActivityUpdates(GoogleApiClient client, long timeInterval, PendingIntent intent);
    public void removeActivityUpdates(GoogleApiClient client, PendingIntent intent);

    public Session.Builder newSessionBuilder();
    public SessionInsertRequest newSessionInsertRequest(Session session, DataSet dataSet);
    public String generateSessionIdentifier(long currentTimeMilliseconds);
    public String generateSessionName();

    public boolean connectionResultHasResolution(ConnectionResult result);
    public void showConnectionResultErrorDialog(ConnectionResult result, Activity activity);
    public void startConnectionResultResolution(ConnectionResult result, Activity activity) throws IntentSender.SendIntentException;
}
