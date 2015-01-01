package com.njackson.activityrecognition;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.njackson.application.modules.PebbleBikeApplication;
import com.njackson.events.ActivityRecognitionService.CurrentState;
import com.squareup.otto.Bus;

import javax.inject.Inject;

/**
 * Created by njackson on 01/01/15.
 */
public class ActivityRecognitionService  extends Service {

    @Inject Bus _bus;

    @Override
    public void onCreate() {
        super.onCreate();
        ((PebbleBikeApplication)getApplication()).inject(this);
        _bus.register(this);

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
}
