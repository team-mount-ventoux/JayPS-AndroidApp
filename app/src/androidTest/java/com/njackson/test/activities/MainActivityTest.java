package com.njackson.test.activities;

import android.content.ComponentName;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.test.ActivityUnitTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.njackson.activities.MainActivity;
import com.njackson.application.modules.PebbleBikeModule;
import com.njackson.events.GPSService.ResetGPSState;
import com.njackson.events.UI.StartButtonTouchedEvent;
import com.njackson.events.UI.StopButtonTouchedEvent;
import com.njackson.gps.GPSService;
import com.njackson.test.application.TestApplication;
import com.njackson.virtualpebble.PebbleService;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
            overrides = true,
            complete = false
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
    private final ArrayList<String> _startedServices = new ArrayList<String>();
    private final ArrayList<String> _stoppedServices = new ArrayList<String>();
    private CountDownLatch _countDownLatch = null;

    @Subscribe
    public void onChangeStateEvent(ResetGPSState state) {
        _stateEvent = state;
        //_countDownLatch.countDown();
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
        setServiceContext();
        setApplication(_app);
    }

    @Override
    protected void tearDown() throws Exception {
        _stoppedServices.clear();
        _startedServices.clear();
        super.tearDown();
    }

    private void setServiceContext() {
        setActivityContext(new ContextWrapper(getInstrumentation().getTargetContext()) {
            @Override
            public ComponentName startService(Intent service) {
                Log.v("mockcontext", "Start service: " + service.toUri(0));
                _startedServices.add(service.getComponent().getClassName());
                return service.getComponent();
            }

            @Override
            public boolean stopService(Intent service) {
                Log.v("mockcontext", "Stop service: " + service.toUri(0));
                _stoppedServices.add(service.getComponent().getClassName());
                return true;
            }
        });
    }

    @SmallTest
    public void testStartButtonTouchedStartsGPS() throws InterruptedException {
        startActivity(new Intent(), null, null);
        getInstrumentation().waitForIdleSync();

        _bus.post(new StartButtonTouchedEvent());

        boolean serviceStarted = waitForServiceToStart(GPSService.class, 2000);
        assertTrue("GPSService should have been started", serviceStarted);
    }

    @SmallTest
    public void testStopButtonTouchedStopsGPS() throws InterruptedException {
        startActivity(new Intent(), null, null);
        getInstrumentation().waitForIdleSync();

        _bus.post(new StopButtonTouchedEvent());

        boolean serviceStarted = waitForServiceToStop(GPSService.class, 2000);
        assertTrue ("GPSService should have been stopped", serviceStarted);
    }

    @SmallTest
    public void testStartButtonTouchedStartsPebbleBikeService() throws InterruptedException {
        startActivity(new Intent(), null, null);
        getInstrumentation().waitForIdleSync();

        _bus.post(new StartButtonTouchedEvent());

        boolean serviceStarted = waitForServiceToStart(PebbleService.class, 2000);
        assertTrue ("PebbleBikeService should have been started", serviceStarted);
    }

    @SmallTest
    public void testStopButtonTouchedStopsPebbleBikeService() throws InterruptedException {
        startActivity(new Intent(), null, null);
        getInstrumentation().waitForIdleSync();

        _bus.post(new StopButtonTouchedEvent());

        boolean serviceStopped = waitForServiceToStop(PebbleService.class, 2000);
        assertTrue ("PebbleBikeService should have been stopped", serviceStopped);
    }

    private boolean waitForServiceToStart(Class serviceClass, int timeout) throws InterruptedException {
        int timer = 0;
        while(timer < timeout) {
            if(_startedServices.contains(GPSService.class.getName())) {
                return true;
            }

            Thread.sleep(100);
            timer += 100;
        }

        return false;
    }

    private boolean waitForServiceToStop(Class serviceClass, int timeout) throws InterruptedException {
        int timer = 0;
        while(timer < timeout) {
            if(_stoppedServices.contains(GPSService.class.getName())) {
                return true;
            }

            Thread.sleep(100);
            timer += 100;
        }

        return false;
    }
}
