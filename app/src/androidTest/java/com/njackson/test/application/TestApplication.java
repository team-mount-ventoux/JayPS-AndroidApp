package com.njackson.test.application;

import android.app.Application;

import com.njackson.application.PebbleBikeApplication;

import dagger.ObjectGraph;

/**
 * Created by server on 30/03/2014.
 */
public class TestApplication extends PebbleBikeApplication {

    public ObjectGraph getObjectGraph() {
        return graph;
    }

}
