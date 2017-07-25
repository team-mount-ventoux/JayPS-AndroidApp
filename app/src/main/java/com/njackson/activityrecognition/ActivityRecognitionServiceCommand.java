package com.njackson.activityrecognition;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.DetectedActivity;
import com.njackson.Constants;
import com.njackson.analytics.IAnalytics;
import com.njackson.application.IInjectionContainer;
import com.njackson.application.modules.ForApplication;
import com.njackson.events.ActivityRecognitionCommand.ActivityRecognitionChangeState;
import com.njackson.events.ActivityRecognitionCommand.ActivityRecognitionStatus;
import com.njackson.events.ActivityRecognitionCommand.NewActivityEvent;
import com.njackson.events.GPSServiceCommand.GPSChangeState;
import com.njackson.events.base.BaseStatus;
import com.njackson.gps.GPSServiceCommand;
import com.njackson.service.IServiceCommand;
import com.njackson.utils.googleplay.IGooglePlayServices;
import com.njackson.utils.services.IServiceStarter;
import com.njackson.utils.time.ITimer;
import com.njackson.utils.time.ITimerHandler;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by njackson on 01/01/15.
 */
public class ActivityRecognitionServiceCommand implements IServiceCommand,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ITimerHandler {

    private static final String TAG = "PB-ActivityRecoService";

    @Inject Bus _bus;
    @Inject IGooglePlayServices _googlePlay;
    @Inject @Named("GoogleActivity") GoogleApiClient _recognitionClient;
    @Inject IServiceStarter _serviceStarter;
    @Inject ITimer _timer;
    @Inject SharedPreferences _sharedPreferences;
    @Inject @ForApplication Context _applicationContext;
    @Inject IAnalytics _parseAnalytics;
    @Inject List<IServiceCommand> _serviceCommands;

    public static final int MILLISECONDS_PER_SECOND = 1000;
    public static final int DETECTION_INTERVAL_SECONDS = 2;
    public static final int DETECTION_INTERVAL_MILLISECONDS = MILLISECONDS_PER_SECOND * DETECTION_INTERVAL_SECONDS;

    private PendingIntent _activityRecognitionPendingIntent;
    private BaseStatus.Status _currentStatus = BaseStatus.Status.NOT_INITIALIZED;

    int _lastActivity = -1;
    int _nbStart = 0;
    int _nbStop = 0;
    boolean _gpsStarted = false;

    @Subscribe
    public void onNewActivityEvent(NewActivityEvent event) {
        boolean autoStart = _sharedPreferences.getBoolean("ACTIVITY_RECOGNITION", false);

        if (autoStart) {
            boolean start = false;
            boolean stop = false;

            switch(event.getActivity().getMostProbableActivity().getType()) {
                case DetectedActivity.IN_VEHICLE:
                case DetectedActivity.STILL:
                    stop = true;
                    break;
                case DetectedActivity.ON_BICYCLE:
                    start = true;
                    break;
                case DetectedActivity.ON_FOOT:
                case DetectedActivity.WALKING:
                case DetectedActivity.RUNNING:
                    if (_sharedPreferences.getBoolean("ACTIVITY_RECOGNITION_WALKING", false)) {
                        start = true;
                    } else {
                        stop = true;
                    }
                    break;

                case DetectedActivity.UNKNOWN:
                case DetectedActivity.TILTING:
                default:
                    break;
            }
            if (start) {
                if (_nbStop > 0) {
                    // stop timer in progress, first cancel it (LocationServices already started?)
                    _timer.cancel();
                }
                _nbStop = 0;
                _nbStart++;
                if (!_timer.getActive()) {
                    _timer.setTimer(Constants.ACTIVITY_RECOGNITION_MOVE_TIME * MILLISECONDS_PER_SECOND, this);
                }

            }
            if (stop) {
                if (_nbStart > 0) {
                    // start timer in progress, first cancel it (LocationServices already stopped?)
                    _timer.cancel();
                }
                _nbStart = 0;
                _nbStop++;
                if (!_timer.getActive()) {
                    _timer.setTimer(Constants.ACTIVITY_RECOGNITION_STILL_TIME * MILLISECONDS_PER_SECOND, this);
                }
            }
            if (_lastActivity != event.getActivity().getMostProbableActivity().getType()) {
                String type = "";
                switch(event.getActivity().getMostProbableActivity().getType()) {
                    case DetectedActivity.ON_BICYCLE:
                        type = "cycling";
                        break;
                    case DetectedActivity.WALKING:
                        type = "walking";
                        break;
                    case DetectedActivity.RUNNING:
                        type = "running";
                        break;
                    case DetectedActivity.ON_FOOT:
                        type = "foot";
                        break;
                    case DetectedActivity.STILL:
                        type = "still";
                        break;
                }
                Log.d(TAG, "_lastActivity: " + _lastActivity + " type=" + type + "_"+ event.getActivity().getMostProbableActivity().getType());
                _lastActivity = event.getActivity().getMostProbableActivity().getType();
//                Map<String, String> params = new HashMap<String, String>();
//                params.put("confidence", Integer.toString(event.getActivity().getMostProbableActivity().getConfidence()));
//                params.put("cycling", Integer.toString(event.getActivity().getActivityConfidence(DetectedActivity.ON_BICYCLE)));
//                params.put("running", Integer.toString(event.getActivity().getActivityConfidence(DetectedActivity.RUNNING)));
//                params.put("walking", Integer.toString(event.getActivity().getActivityConfidence(DetectedActivity.WALKING)));
//                _parseAnalytics.trackEvent("activity_" + type + "_"+ event.getActivity().getMostProbableActivity().getType(), params);
            }
        }
    }

    @Subscribe
    public void onChangeState(ActivityRecognitionChangeState event) {
        switch (event.getState()) {
            case START:
                if(_currentStatus != BaseStatus.Status.STARTED) {
                    start();
                }
                break;
            case STOP:
                if(_currentStatus != BaseStatus.Status.STOPPED) {
                    stop();
                }
        }
    }
    @Subscribe
    public void onGPSChangeState(GPSChangeState event) {
        switch(event.getState()) {
            case START:
                _gpsStarted = true;
                break;
            case STOP:
                _gpsStarted = false;
                break;
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

    public void start() {
        Log.d(TAG,"Started Activity Recognition Service");

        if(!checkGooglePlayServices()) {
            _currentStatus = BaseStatus.Status.UNABLE_TO_START;
            _bus.post(new ActivityRecognitionStatus(_currentStatus, false));
            return;
        }

        registerRecognitionCallbacks();
        createIntentService();
        connectToGooglePlayServices();

        _currentStatus = BaseStatus.Status.STARTED;
        _bus.post(new ActivityRecognitionStatus(_currentStatus));
    }

    public void stop (){
        // TODO(jay) stop me only if running
        Log.d(TAG,"Destroy Activity Recognition Service");

        _recognitionClient.unregisterConnectionCallbacks(this);
        _recognitionClient.unregisterConnectionFailedListener(this);

        _googlePlay.removeActivityUpdates(_recognitionClient,_activityRecognitionPendingIntent);
        _recognitionClient.disconnect();

        _currentStatus = BaseStatus.Status.STOPPED;
        _bus.post(new ActivityRecognitionStatus(_currentStatus));
    }

    private void createIntentService() {
        Intent i = new Intent(_applicationContext, ActivityRecognitionIntentService.class);
        _activityRecognitionPendingIntent = PendingIntent.getService(_applicationContext, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void connectToGooglePlayServices() {
        _recognitionClient.connect();
    }

    private void registerRecognitionCallbacks() {
        _recognitionClient.registerConnectionCallbacks(this);
        _recognitionClient.registerConnectionFailedListener(this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG,"Connected to Activity Service");
        _googlePlay.requestActivityUpdates(_recognitionClient, DETECTION_INTERVAL_MILLISECONDS, _activityRecognitionPendingIntent);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG,"Connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG,"Connection failed");
    }

    private boolean checkGooglePlayServices() {
        return (_googlePlay.
                isGooglePlayServicesAvailable(_applicationContext) == ConnectionResult.SUCCESS);
    }

    @Override
    public void handleTimeout() {
        Log.d(TAG, "_gpsStarted:" + _gpsStarted);

        Map<String, String> params = new HashMap<String, String>();
        if (_nbStart > 0) {
            Log.d(TAG, "Starting location + _nbStart:" + _nbStart);
            if (!_gpsStarted) {
                _serviceStarter.startLocationServices();
                params.put("nb", Integer.toString(_nbStart));
                _parseAnalytics.trackEvent("auto_start", params);
            }
        } else if (_nbStop > 0) {
            Log.d(TAG, "Stopping location + nbStop:" + _nbStop);
            if (_gpsStarted) {
                _serviceStarter.stopLocationServices();
                params.put("nb", Integer.toString(_nbStop));
                _parseAnalytics.trackEvent("auto_stop", params);
            }
        } else {
            Log.d(TAG, "Error handleTimeout");
        }
        _nbStart = _nbStop = 0;
    }
}
