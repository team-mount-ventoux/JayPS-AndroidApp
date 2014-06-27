package com.njackson.test.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.activities.MainActivity;
import com.njackson.application.PebbleBikeModule;
import com.njackson.gps.GPSService;
import com.njackson.test.application.TestApplication;
import com.squareup.otto.Bus;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by server on 30/03/2014.
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    @Inject Bus _bus;

    @Module(
            includes = PebbleBikeModule.class,
            injects = MainActivityTest.class,
            overrides = true
    )
    static class TestModule {
        @Provides
        @Singleton
        LocationManager provideLocationManager() {
            return mock(LocationManager.class);
        }

        @Provides
        @Singleton
        SharedPreferences provideSharedPreferences() {
            return mock(SharedPreferences.class);
        }
    }

    public MainActivityTest(Class<MainActivity> activityClass) {
        super(activityClass);
    }

    public MainActivityTest() { super(MainActivity.class); }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());
        TestApplication app = (TestApplication)getInstrumentation().getTargetContext().getApplicationContext();
        app.setObjectGraph(ObjectGraph.create(TestModule.class));
        app.inject(this);
    }

    @SmallTest
    public void whenStartButtonTouchedEventReceivedFiresStartGPS() {

    }

}
