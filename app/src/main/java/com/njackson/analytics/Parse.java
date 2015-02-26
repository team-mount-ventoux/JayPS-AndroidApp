package com.njackson.analytics;

import android.app.Application;
import android.content.Intent;

import com.parse.ParseAnalytics;
import com.parse.ParseCrashReporting;

/**
 * Created by njackson on 23/12/14.
 */
public class Parse implements IAnalytics {
    @Override
    public void enable(Application application) {
        ParseCrashReporting.enable(application);
        com.parse.Parse.initialize(application, "NIwEkYlaiestozg1sel0U9cDQk0AR5qLi8sSouxn", "5FzfXeSMyWnaXmy41zVzatF1dwdjLfP11gtLi5jf");
    }

    @Override
    public void trackAppOpened(Intent intent) {
        ParseAnalytics.trackAppOpenedInBackground(intent);
    }

    @Override
    public void trackAppDestroy() {
        ParseAnalytics.trackEventInBackground("AppDestroy");
    }

    @Override
    public void trackLowMemory() { ParseAnalytics.trackEventInBackground("LowMemory"); }

}
