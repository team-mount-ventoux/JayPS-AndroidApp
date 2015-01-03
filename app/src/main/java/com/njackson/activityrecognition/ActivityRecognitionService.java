package com.njackson.activityrecognition;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.DetectedActivity;
import com.njackson.Constants;
import com.njackson.application.PebbleBikeApplication;
import com.njackson.events.ActivityRecognitionService.CurrentState;
import com.njackson.events.ActivityRecognitionService.NewActivityEvent;
import com.njackson.utils.googleplay.IGooglePlayServices;
import com.njackson.utils.services.IServiceStarter;
import com.njackson.utils.timer.ITimer;
import com.njackson.utils.timer.ITimerHandler;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

/**
 * Created by njackson on 01/01/15.
 */
public class ActivityRecognitionService  extends Service implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ITimerHandler {

    @Inject Bus _bus;
    @Inject IGooglePlayServices _googlePlay;
    @Inject GoogleApiClient _recognitionClient;
    @Inject IServiceStarter _serviceStarter;
    @Inject ITimer _timer;

    public static final int MILLISECONDS_PER_SECOND = 1000;
    public static final int DETECTION_INTERVAL_SECONDS = 20;
    public static final int DETECTION_INTERVAL_MILLISECONDS = MILLISECONDS_PER_SECOND * DETECTION_INTERVAL_SECONDS;

    private PendingIntent _activityRecognitionPendingIntent;

    @Subscribe
    public void onNewActivityEvent(NewActivityEvent event) {
        if(event.getActivityType() != DetectedActivity.STILL) {
            _serviceStarter.startLocationServices();
        } else {
            if(!_timer.getActive()) {
                _timer.setTimer(Constants.ACTIVITY_RECOGNITON_STILL_TIME, this);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        ((PebbleBikeApplication)getApplication()).inject(this);

        _bus.register(this);

        if(!checkGooglePlayServices()) {
            _bus.post(new CurrentState(CurrentState.State.PLAY_SERVICES_NOT_AVAILABLE));
            return;
        }

        registerRecognitionCallbacks();
        connectToGooglePlayServices();

        _bus.post(new CurrentState(CurrentState.State.STARTED));
    }

    private void connectToGooglePlayServices() {
        _recognitionClient.connect();
    }

    private void registerRecognitionCallbacks() {
        _recognitionClient.registerConnectionCallbacks(this);
        _recognitionClient.registerConnectionFailedListener(this);
    }

    @Override
    public void onDestroy (){
        _bus.unregister(this);
        _recognitionClient.unregisterConnectionCallbacks(this);
        _recognitionClient.unregisterConnectionFailedListener(this);

        _googlePlay.removeActivityUpdates(_recognitionClient,_activityRecognitionPendingIntent);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Intent i = new Intent(this, ActivityRecognitionIntentService.class);
        _activityRecognitionPendingIntent = PendingIntent.getService(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        _googlePlay.requestActivityUpdates(_recognitionClient, DETECTION_INTERVAL_MILLISECONDS, _activityRecognitionPendingIntent);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private boolean checkGooglePlayServices() {
        return (_googlePlay.
                isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS);
    }

    @Override
    public void handleTimeout() {
        _serviceStarter.stopLocationServices();
    }
}
