package com.njackson.utils.googleplay;

import android.app.PendingIntent;
import android.content.Context;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.data.Session;

/**
 * Created by njackson on 02/01/15.
 */
public interface IGooglePlayServices {
    public int isGooglePlayServicesAvailable(Context context);
    public void requestActivityUpdates(GoogleApiClient client, long timeInterval, PendingIntent intent);
    public void removeActivityUpdates(GoogleApiClient client, PendingIntent intent);
    public Session.Builder newSessionBuilder();
    public String generateSessionIdentifier(long currentTimeMilliseconds);
    public String generateSessionName();
}
