package com.njackson.activityrecognition;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.njackson.application.modules.PebbleBikeApplication;
import com.njackson.events.ActivityRecognitionService.CurrentState;
import com.njackson.utils.googleplay.IGooglePlayServices;
import com.squareup.otto.Bus;

import javax.inject.Inject;

/**
 * Created by njackson on 01/01/15.
 */
public class ActivityRecognitionService  extends Service {

    @Inject Bus _bus;
    @Inject IGooglePlayServices _googlePlay;

    @Override
    public void onCreate() {
        super.onCreate();
        ((PebbleBikeApplication)getApplication()).inject(this);
        _bus.register(this);

        if(!checkGooglePlayServices()) {
            _bus.post(new CurrentState(CurrentState.State.PLAY_SERVICES_NOT_AVAILABLE));
            return;
        }

        _bus.post(new CurrentState(CurrentState.State.STARTED));
    }

    @Override
    public void onDestroy (){
        Log.d("MAINTEST", "Stopped Activity Recognition Service");
        _bus.unregister(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private boolean checkGooglePlayServices() {
        return (_googlePlay.
                isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS);
    }
}
