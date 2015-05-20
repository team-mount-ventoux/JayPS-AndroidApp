package com.njackson.application;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import com.njackson.analytics.IAnalytics;
import com.njackson.application.modules.AndroidModule;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.ObjectGraph;

/**
 * Created by server on 30/03/2014.
 */
public class PebbleBikeApplication extends Application implements IInjectionContainer {

    private static final String TAG = "PB-PebbleBikeApp";

    protected ObjectGraph graph;

    @Inject IAnalytics _parseAnalytics;
    @Inject SharedPreferences _sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onLowMemory() {
        Log.d(TAG,"Low Memory");
        _parseAnalytics.trackLowMemory();
        super.onLowMemory();
    }

    protected List<Object> getModules() {
        List<Object> modules = new ArrayList<Object>();
        modules.add(new AndroidModule(this));
        return modules;
    }

    public void inject(Object object) {
        if(graph == null) {
            createObjectGraph();
        }
        graph.inject(object);
    }

    private void createObjectGraph() {
        graph = ObjectGraph.create(getModules().toArray());
        setupAnalytics();
    }

    protected void setupAnalytics() {
        inject(this);
        if (_sharedPreferences.getBoolean("PREF_REPORT_CRASH", false)) {
            _parseAnalytics.enable(this);
        }
    }

}
