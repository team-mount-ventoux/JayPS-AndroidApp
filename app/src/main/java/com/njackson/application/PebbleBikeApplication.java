package com.njackson.application;

import android.app.Application;
import android.util.Log;

import com.njackson.application.modules.AndroidModule;
import com.parse.Parse;
import com.parse.ParseCrashReporting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dagger.ObjectGraph;

/**
 * Created by server on 30/03/2014.
 */
public class PebbleBikeApplication extends Application implements IInjectionContainer {

    private static final String TAG = "PB-PebbleBikeApplication";

    protected ObjectGraph graph;

    @Override public void onCreate() {
        super.onCreate();

        ParseCrashReporting.enable(this);
        Parse.initialize(this, "NIwEkYlaiestozg1sel0U9cDQk0AR5qLi8sSouxn", "5FzfXeSMyWnaXmy41zVzatF1dwdjLfP11gtLi5jf");
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
        Log.d(TAG, "Create object graph");
        graph = ObjectGraph.create(getModules().toArray());
    }

}
