package com.njackson.utils.googleplay;

import android.app.PendingIntent;
import android.content.Context;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.location.ActivityRecognition;

/**
 * Created by njackson on 02/01/15.
 */
public class GooglePlayServices implements IGooglePlayServices {
    @Override
    public int isGooglePlayServicesAvailable(Context context) {
        return GooglePlayServicesUtil.
                isGooglePlayServicesAvailable(context);
    }

    @Override
    public void requestActivityUpdates(GoogleApiClient client, long timeInterval, PendingIntent intent) {
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(client,timeInterval,intent);
    }

    @Override
    public void removeActivityUpdates(GoogleApiClient client, PendingIntent intent) {
        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(client,intent);
    }

    @Override
    public Session.Builder newSessionBuilder() {
        return new Session.Builder();
    }
}
