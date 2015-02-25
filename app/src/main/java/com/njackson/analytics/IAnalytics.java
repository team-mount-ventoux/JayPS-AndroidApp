package com.njackson.analytics;

import android.app.Application;
import android.content.Intent;

import com.njackson.application.PebbleBikeApplication;

/**
 * Created by njackson on 23/12/14.
 */
public interface IAnalytics {
    void enable(Application application);
    void trackAppOpened(Intent intent);
    void trackAppDestroy();
    void trackALowMemory();
}
