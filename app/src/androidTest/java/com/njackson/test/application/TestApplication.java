package com.njackson.test.application;

import android.util.Log;

import com.njackson.application.PebbleBikeApplication;
import dagger.ObjectGraph;

/**
 * Created by server on 30/03/2014.
 */
public class TestApplication extends PebbleBikeApplication {

    private static final String TAG = "PB-TestApplication";

    public ObjectGraph getObjectGraph() {
        return graph;
    }
    public void setObjectGraph(ObjectGraph graph) {
        Log.d(TAG, "set object graph");
        this.graph = graph;
    }

    public void setupAnalytics() {
        super.setupAnalytics();
    }


}
