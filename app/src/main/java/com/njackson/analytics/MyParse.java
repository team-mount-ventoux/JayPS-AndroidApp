package com.njackson.analytics;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.parse.Parse;
import com.parse.ParseAnalytics;

import java.util.Iterator;
import java.util.Map;
//import com.parse.ParseCrashReporting;

/**
 * Created by njackson on 23/12/14.
 */
public class MyParse implements IAnalytics {

    private boolean _enabled = false;

    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    public void enable(Application application) {
        //ParseCrashReporting.enable(application);
        com.parse.Parse.initialize(application);
        _enabled = true;

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(application);
    }

    @Override
    public void trackAppOpened(Intent intent) {
        if (_enabled) {
            ParseAnalytics.trackAppOpenedInBackground(intent);
        }
    }

    @Override
    public void trackAppDestroy() {
        if (_enabled) {
            ParseAnalytics.trackEventInBackground("AppDestroy");
        }
    }

    @Override
    public void trackLowMemory() {
        if (_enabled) {
            ParseAnalytics.trackEventInBackground("LowMemory");
        }
    }

    @Override
    public void trackSkippedMessage() {
        if (_enabled) {
            ParseAnalytics.trackEventInBackground("SkippedMessage");
        }
    }

    @Override
    public void trackEvent(String name) {
        if (_enabled) {
            ParseAnalytics.trackEventInBackground(name);

            Bundle params = new Bundle();
            mFirebaseAnalytics.logEvent(name, params);
        }
    }
    @Override
    public void trackEvent(String name, Map<String, String> dimensions) {
        if (_enabled) {
            ParseAnalytics.trackEventInBackground(name, dimensions);

            Bundle params = new Bundle();
            Iterator it = dimensions.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry param = (Map.Entry) it.next();
                params.putString(param.getKey().toString(), param.getValue().toString());
            }
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, params);
        }
    }

}
