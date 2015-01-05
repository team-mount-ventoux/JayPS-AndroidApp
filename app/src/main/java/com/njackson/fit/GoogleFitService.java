package com.njackson.fit;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.njackson.application.PebbleBikeApplication;
import com.njackson.events.GoogleFitService.CurrentState;
import com.squareup.otto.Bus;

import javax.inject.Inject;

/**
 * Created by njackson on 05/01/15.
 */
public class GoogleFitService extends Service {

    @Inject Bus _bus;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleCommand(intent);
        // ensures that if the service is recycled then it is restarted with the same refresh interval
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((PebbleBikeApplication)getApplication()).inject(this);
        _bus.register(this);
    }

    @Override
    public void onDestroy (){
        _bus.unregister(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleCommand(Intent intent) {
        _bus.post(new CurrentState(CurrentState.State.STARTED));
    }
}
