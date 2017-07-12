package com.njackson.analytics;

import android.app.Application;
import android.content.Intent;

import com.parse.Parse;
import com.parse.ParseAnalytics;

import java.util.Map;
//import com.parse.ParseCrashReporting;

/**
 * Created by njackson on 23/12/14.
 */
public class MyParse implements IAnalytics {

    private boolean _enabled = false;

    @Override
    public void enable(Application application) {
        //ParseCrashReporting.enable(application);
        com.parse.Parse.initialize(application);
        _enabled = true;
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
        }
    }
    @Override
    public void trackEvent(String name, Map<String, String> dimensions) {
        if (_enabled) {
            ParseAnalytics.trackEventInBackground(name, dimensions);
        }
    }

}
