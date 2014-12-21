package com.njackson.test.activities;

import android.app.ActivityManager;
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
import com.njackson.application.modules.PebbleBikeModule;
import com.njackson.application.modules.PebbleServiceModule;
import com.njackson.events.GPSService.ResetGPSState;
import com.njackson.events.UI.StartButtonTouchedEvent;
import com.njackson.events.UI.StopButtonTouchedEvent;
import com.njackson.gps.GPSService;
import com.njackson.test.application.TestApplication;
import com.njackson.virtualpebble.IMessageManager;
import com.njackson.virtualpebble.MessageManager;
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
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by server on 30/03/2014.
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    @Inject Bus _bus;
    @Inject SharedPreferences _mockPreferences;

    private MainActivity _activity;
    private TestApplication _app;

    private SharedPreferences.Editor _mockEditor;

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
        SharedPreferences provideSharedPreferences() { return mock(SharedPreferences.class); }

        @Provides
        public IMessageManager providesMessageManager() { return mock(IMessageManager.class); }
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

        setupMocks();

        _activity = getActivity();
        getInstrumentation().waitForIdleSync();
    }

    private void setupMocks() {
        _mockEditor = mock(SharedPreferences.Editor.class, RETURNS_DEEP_STUBS);
        when(_mockPreferences.edit()).thenReturn(_mockEditor);
    }

    @Override
    protected void tearDown() throws Exception {
        _stoppedServices.clear();
        _startedServices.clear();
        super.tearDown();
    }

    @SmallTest
    public void testStartButtonTouchedStartsGPS() throws Exception {
        _bus.post(new StartButtonTouchedEvent());

        boolean serviceStarted = waitForServiceToStart(GPSService.class, 2000);
        assertTrue("GPSService should have been started", serviceStarted);
    }

    @SmallTest
    public void testStopButtonTouchedStopsGPS() throws Exception {
        startServiceAndWaitForReady(GPSService.class);

        _bus.post(new StopButtonTouchedEvent());

        boolean serviceStopped = waitForServiceToStop(GPSService.class, 2000);
        assertTrue ("GPSService should have been stopped", serviceStopped);
    }

    @SmallTest
    public void testStartButtonTouchedStartsPebbleBikeService() throws Exception {
        _bus.post(new StartButtonTouchedEvent());

        boolean serviceStarted = waitForServiceToStart(PebbleService.class, 2000);
        assertTrue ("PebbleBikeService should have been started", serviceStarted);
    }

    @SmallTest
    public void testStopButtonTouchedStopsPebbleBikeService() throws Exception {
        startServiceAndWaitForReady(PebbleService.class);

        _bus.post(new StopButtonTouchedEvent());

        boolean serviceStopped = waitForServiceToStop(PebbleService.class, 2000);
        assertTrue ("PebbleBikeService should have been stopped", serviceStopped);
    }

    private boolean waitForServiceToStart(Class serviceClass, int timeout) throws Exception {
        int timer = 0;
        while(timer < timeout) {
            if(serviceRunning(serviceClass)) {
                return true;
            }

            Thread.sleep(100);
            timer += 100;
        }

        throw new Exception("Timeout waiting for Service to Start");
    }

    private void startServiceAndWaitForReady(Class clazz) throws Exception {
        _activity.startService(new Intent(_activity,clazz));
        boolean serviceStarted = waitForServiceToStart(clazz, 2000);
    }

    private boolean waitForServiceToStop(Class serviceClass, int timeout) throws Exception {
        int timer = 0;
        while(timer < timeout) {
            if(!serviceRunning(serviceClass)) {
                return true;
            }

            Thread.sleep(100);
            timer += 100;
        }

        throw new Exception("Timeout waiting for Service to Stop");
    }

    private boolean serviceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) _activity.getSystemService(_activity.getApplicationContext().ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
