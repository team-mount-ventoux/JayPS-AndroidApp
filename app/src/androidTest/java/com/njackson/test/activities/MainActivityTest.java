package com.njackson.test.activities;

import android.content.ComponentName;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.ActivityUnitTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.njackson.activities.MainActivity;
import com.njackson.application.PebbleBikeModule;
import com.njackson.events.GPSService.ResetGPSState;
import com.njackson.events.UI.StartButtonTouchedEvent;
import com.njackson.gps.GPSService;
import com.njackson.test.application.TestApplication;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created by server on 30/03/2014.
 */
public class MainActivityTest extends ActivityUnitTestCase<MainActivity> {

    @Inject Bus _bus;
    private MainActivity _activity;
    private TestApplication _app;

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

    private ResetGPSState _stateEvent;

    @Subscribe
    public void onChangeStateEvent(ResetGPSState state) {
        _stateEvent = state;
    }

    public MainActivityTest(Class<MainActivity> activityClass) {
        super(activityClass);
    }

    public MainActivityTest() { super(MainActivity.class); }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());
        _app = (TestApplication)getInstrumentation().getTargetContext().getApplicationContext();
        _app.setObjectGraph(ObjectGraph.create(TestModule.class));
        _app.inject(this);
        _bus.register(this);
    }

    @SmallTest
    public void testStartButtonTouchedStartsGPS() throws InterruptedException {
        final ArrayList<String> services = new ArrayList<String>();

        setActivityContext(new ContextWrapper(getInstrumentation().getTargetContext()) {
            @Override
            public ComponentName startService(Intent service) {
                Log.v("mockcontext", "Start service: " + service.toUri(0));
                services.add(service.getComponent().getClassName());
                return service.getComponent();
            }
        });

        setApplication(_app);

        startActivity(new Intent(), null, null);
        getInstrumentation().waitForIdleSync();

        _bus.post(new StartButtonTouchedEvent());
        Thread.sleep(100);
        assertTrue ("GPSService should have been started", services.contains(GPSService.class.getName()));
    }

}
