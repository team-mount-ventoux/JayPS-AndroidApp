package com.njackson.application;

import android.app.Application;
import android.util.Log;

import com.njackson.application.modules.AndroidModule;
import com.njackson.application.modules.PebbleBikeModule;
import com.njackson.application.modules.PebbleServiceModule;
import com.parse.Parse;
import com.parse.ParseCrashReporting;

import java.util.Arrays;
import java.util.List;

import dagger.ObjectGraph;

/**
 * Created by server on 30/03/2014.
 */
public class PebbleBikeApplication extends Application {

    protected ObjectGraph graph;

    @Override public void onCreate() {
        super.onCreate();

        ParseCrashReporting.enable(this);
        Parse.initialize(this, "NIwEkYlaiestozg1sel0U9cDQk0AR5qLi8sSouxn", "5FzfXeSMyWnaXmy41zVzatF1dwdjLfP11gtLi5jf");
    }

    protected List<Object> getModules() {
        return Arrays.asList(
                new AndroidModule(this),
                new PebbleBikeModule(),
                new PebbleServiceModule(getApplicationContext())
        );
    }

    public void inject(Object object) {
        if(graph == null) {
            createObjectGraph();
        }
        graph.inject(object);
    }

    private void createObjectGraph() {
        Log.d("MAINTEST", "Create object graph");
        graph = ObjectGraph.create(getModules().toArray());
    }

}
