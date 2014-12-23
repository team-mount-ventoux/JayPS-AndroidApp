package com.njackson.analytics;

import android.content.Intent;

import com.parse.ParseAnalytics;

/**
 * Created by njackson on 23/12/14.
 */
public class Parse implements IAnalytics {
    @Override
    public void trackAppOpened(Intent intent) {
        ParseAnalytics.trackAppOpened(intent);
    }
}
