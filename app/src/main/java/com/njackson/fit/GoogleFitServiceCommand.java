package com.njackson.fit;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.DetectedActivity;
import com.njackson.application.IInjectionContainer;
import com.njackson.events.ActivityRecognitionCommand.NewActivityEvent;
import com.njackson.events.GoogleFitCommand.GoogleFitChangeState;
import com.njackson.events.GoogleFitCommand.GoogleFitStatus;
import com.njackson.events.base.BaseStatus;
import com.njackson.service.IServiceCommand;
import com.njackson.utils.googleplay.IGoogleFitSessionManager;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by njackson on 05/01/15.
 */
public class GoogleFitServiceCommand implements IServiceCommand, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "GoogleFitService";
    @Inject Bus _bus;
    @Inject @Named("GoogleFit") GoogleApiClient _googleAPIClient;
    @Inject IGoogleFitSessionManager _sessionManager;

    private BaseStatus.Status _currentStatus = BaseStatus.Status.NOT_INITIALIZED;

    @Subscribe
    public void onNewActivityEvent(NewActivityEvent event) {
        if(_currentStatus == BaseStatus.Status.STARTED) {
            switch (event.getActivityType()) {
                case DetectedActivity.ON_BICYCLE:
                case DetectedActivity.RUNNING:
                case DetectedActivity.WALKING:
                case DetectedActivity.ON_FOOT:
                    _sessionManager.addDataPoint(new Date().getTime(), event.getActivityType());
                    break;
            }
        }
    }

    @Subscribe
    public void onChangeState(GoogleFitChangeState event) {
        switch(event.getState()) {
            case START:
                if(_currentStatus != BaseStatus.Status.STARTED)
                    start();
                break;
            case STOP:
                if(_currentStatus != BaseStatus.Status.STOPPED)
                    stop();
        }
    }

    @Override
    public void execute(IInjectionContainer container) {
        container.inject(this);
        _bus.register(this);
        _currentStatus = BaseStatus.Status.INITIALIZED;
    }

    @Override
    public void dispose() {
        _bus.unregister(this);
    }

    @Override
    public BaseStatus.Status getStatus() {
        return _currentStatus;
    }

    private void start() {
        _googleAPIClient.registerConnectionCallbacks(this);
        _googleAPIClient.registerConnectionFailedListener(this);
        _googleAPIClient.connect();

        _currentStatus = BaseStatus.Status.STARTED;
        _bus.post(new GoogleFitStatus(_currentStatus));
        Log.d(TAG,"Start");
    }

    public void stop () {
        _googleAPIClient.unregisterConnectionFailedListener(this);
        _googleAPIClient.unregisterConnectionCallbacks(this);

        stopRecordingSession();

        _currentStatus = BaseStatus.Status.STOPPED;
        _bus.post(new GoogleFitStatus(_currentStatus));
        Log.d(TAG,"Stop");
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
        _currentStatus = BaseStatus.Status.UNABLE_TO_START;
        _bus.post(new GoogleFitStatus(_currentStatus, connectionResult));
    }
    /* END GOOGLE CLIENT DELEGATE METHODS */

    private void startRecordingSession() {
        long startTime = new Date().getTime();
        Log.d(TAG,"Start Recording Sessions");
        _sessionManager.startSession(startTime,_googleAPIClient);
    }

    private void stopRecordingSession() {
        if(_googleAPIClient.isConnected()) {
            Log.d(TAG,"Stopped Recording Sessions");
            _sessionManager.saveActiveSession(new Date().getTime());
        }
    }
}
