package com.njackson.application.modules;

import android.app.Application;
import android.util.Log;

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
    }

    protected List<Object> getModules() {
        return Arrays.asList(
                new AndroidModule(this),
                new PebbleBikeModule(),
                new PebbleServiceModule()
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
