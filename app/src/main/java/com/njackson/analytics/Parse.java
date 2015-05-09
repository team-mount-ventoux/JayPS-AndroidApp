package com.njackson.analytics;

import android.app.Application;
import android.content.Intent;

import com.parse.ParseAnalytics;
import com.parse.ParseCrashReporting;

/**
 * Created by njackson on 23/12/14.
 */
public class Parse implements IAnalytics {

    private boolean _enabled = false;

    @Override
    public void enable(Application application) {
        ParseCrashReporting.enable(application);
        com.parse.Parse.initialize(application, "NIwEkYlaiestozg1sel0U9cDQk0AR5qLi8sSouxn", "5FzfXeSMyWnaXmy41zVzatF1dwdjLfP11gtLi5jf");
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
}
