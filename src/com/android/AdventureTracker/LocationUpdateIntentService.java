package com.android.AdventureTracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created with IntelliJ IDEA.
 * User: server
 * Date: 21/05/2013
 * Time: 14:45
 * To change this template use File | Settings | File Templates.
 */
public class LocationUpdateIntentService extends BroadcastReceiver {

    public LocationUpdateIntentService(){
        super();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //To change body of implemented methods use File | Settings | File Templates.
        Log.d("LocationUpdateIntentService","Got Location");

        //Location loc = intent.getParcelableExtra("KEY_LOCATION_CHANGED");
    }
}
