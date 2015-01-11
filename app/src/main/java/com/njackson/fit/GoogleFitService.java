package com.njackson.fit;

import android.app.Service;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.RecordingApi;
import com.google.android.gms.fitness.SessionsApi;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.data.Subscription;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.SessionInsertRequest;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.result.DataSourcesResult;
import com.google.android.gms.fitness.result.ListSubscriptionsResult;
import com.google.android.gms.fitness.result.SessionReadResult;
import com.google.android.gms.fitness.result.SessionStopResult;
import com.njackson.activities.MainActivity;
import com.njackson.application.PebbleBikeApplication;
import com.njackson.events.status.GoogleFitStatus;
import com.njackson.utils.googleplay.IGooglePlayServices;
import com.squareup.otto.Bus;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by njackson on 05/01/15.
 */
public class GoogleFitService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "GoogleFitService";
    @Inject Bus _bus;
    @Inject @Named("GoogleFit") GoogleApiClient _googleAPIClient;
    @Inject RecordingApi _recordingApi;
    @Inject SessionsApi _sessionsApi;
    @Inject IGooglePlayServices _playServices;
    private Session _session;
    private String _sessionIdentifier;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleCommand(intent);
        // ensures that if the service is recycled then it is restarted with the same refresh interval
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Create GoogleFit Service");
        ((PebbleBikeApplication)getApplication()).inject(this);
        _bus.register(this);
    }

    @Override
    public void onDestroy (){
        _bus.unregister(this);

        _googleAPIClient.unregisterConnectionFailedListener(this);
        _googleAPIClient.unregisterConnectionCallbacks(this);

        stopRecordingSession();

        Log.d(TAG,"Destroy GoogleFit Service");

        super.onDestroy();
    }

    private void stopRecordingSession() {
        if(_googleAPIClient.isConnected()) {
            PendingResult<SessionStopResult> pendingResult = _sessionsApi.stopSession(_googleAPIClient, _sessionIdentifier);

            Log.d(TAG,"Stopped Recording Sessions");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleCommand(Intent intent) {
        _googleAPIClient.registerConnectionCallbacks(this);
        _googleAPIClient.registerConnectionFailedListener(this);
        _googleAPIClient.connect();
        _bus.post(new GoogleFitStatus(GoogleFitStatus.State.SERVICE_STARTED));
    }

    /* GOOOGLE CLIENT DELEGATE METHODS */
    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG,"Connected");
        startRecordingSession();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG,"Connection Suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG,"Connection Failed");
        _bus.post(new GoogleFitStatus(GoogleFitStatus.State.GOOGLEFIT_CONNECTION_FAILED, connectionResult));
    }
    /* END GOOGLE CLIENT DELEGATE METHODS */

    private void startRecordingSession() {
        long startTime = new Date().getTime();
        //String description = "";
        String sessionName = _playServices.generateSessionName();
        _sessionIdentifier = _playServices.generateSessionIdentifier(startTime);

        _session = _playServices.newSessionBuilder()
                .setName(sessionName)
                .setIdentifier(_sessionIdentifier)
                //.setDescription(description)
                .setStartTime(startTime, TimeUnit.MILLISECONDS)
                        // optional - if your app knows what activity:
                //.setActivity(FitnessActivities.RUNNING)
                .build();

        _sessionsApi.startSession(_googleAPIClient, _session);
    }
}
