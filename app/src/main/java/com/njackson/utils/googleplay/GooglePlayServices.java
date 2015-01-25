package com.njackson.utils.googleplay;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.IntentSender;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.SessionInsertRequest;
import com.google.android.gms.location.ActivityRecognition;
import com.njackson.Constants;

/**
 * Created by njackson on 02/01/15.
 */
public class GooglePlayServices implements IGooglePlayServices {
    private int REQUEST_OAUTH = 1;

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
        if(client.isConnected()) {
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(client, intent);
        }
    }

    @Override
    public Session.Builder newSessionBuilder() {
        return new Session.Builder();
    }

    @Override
    public SessionInsertRequest newSessionInsertRequest(Session session, DataSet dataSet) {
        return new SessionInsertRequest.Builder()
                .addDataSet(dataSet)
                .setSession(session)
                .build();
    }

    @Override
    public String generateSessionIdentifier(long currentTimeMilliseconds) {
        return Constants.GOOGLE_FIT_SESSION_IDENTIFIER_PREFIX + currentTimeMilliseconds;
    }

    @Override
    public String generateSessionName() {
        return Constants.GOOGLE_FIT_SESSION_NAME;
    }

    @Override
    public boolean connectionResultHasResolution(ConnectionResult result) {
        return result.hasResolution();
    }

    @Override
    public void showConnectionResultErrorDialog(ConnectionResult result, Activity activity) {
        GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(),activity,0).show();
    }

    @Override
    public void startConnectionResultResolution(ConnectionResult result, Activity activity) throws IntentSender.SendIntentException{
        try {
            result.startResolutionForResult(activity, REQUEST_OAUTH);
        }catch (IntentSender.SendIntentException e) {
            throw e;
        }
    }
}
