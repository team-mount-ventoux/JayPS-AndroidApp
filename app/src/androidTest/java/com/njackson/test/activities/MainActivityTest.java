package com.njackson.test.activities;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.ActivityUnitTestCase;
import android.test.UiThreadTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.njackson.activities.MainActivity;
import com.njackson.activityrecognition.ActivityRecognitionService;
import com.njackson.analytics.IAnalytics;
import com.njackson.application.modules.PebbleBikeModule;
import com.njackson.application.modules.PebbleServiceModule;
import com.njackson.events.GPSService.ResetGPSState;
import com.njackson.events.UI.StartButtonTouchedEvent;
import com.njackson.events.UI.StopButtonTouchedEvent;
import com.njackson.gps.GPSService;
import com.njackson.live.LiveService;
import com.njackson.live.LiveTracking;
import com.njackson.test.application.TestApplication;
import com.njackson.test.testUtils.Services;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by server on 30/03/2014.
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    @Inject Bus _bus;
    @Inject SharedPreferences _mockPreferences;
    @Inject IAnalytics _mockAnalytics;

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
        @Provides @Singleton
        LocationManager provideLocationManager() {
            return mock(LocationManager.class);
        }

        @Provides @Singleton
        SharedPreferences provideSharedPreferences() { return mock(SharedPreferences.class); }

        @Provides
        public IMessageManager providesMessageManager() { return mock(IMessageManager.class); }

        @Provides @Singleton
        public IAnalytics providesAnalytics() { return mock(IAnalytics.class); }

        @Provides @Singleton
        SensorManager provideSensorManager() { return mock(SensorManager.class); }

        @Provides @Singleton
        GoogleApiClient provideActivityRecognitionClient() { return mock(GoogleApiClient.class); }
    }

    private ResetGPSState _stateEvent;

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

        Log.d("MAINTEST", "Setup Complete");
    }

    private void setupMocks() {
        _mockEditor = mock(SharedPreferences.Editor.class, RETURNS_DEEP_STUBS);
        when(_mockPreferences.edit()).thenReturn(_mockEditor);
    }

    @SmallTest
    public void testSendsTrackAppOpenedAnalyticsOnCreate() {
        _activity = getActivity();

        verify(_mockAnalytics,times(1)).trackAppOpened(any(Intent.class));
    }

    @SmallTest
    public void testRegistersForSharedPreferencesUpdatesOnCreate() {
        _activity = getActivity();

        verify(_mockPreferences,times(1)).registerOnSharedPreferenceChangeListener(any(MainActivity.class));
    }

    @UiThreadTest
    public void testUnRegistersForSharedPreferencesUpdatesOnDestroy() {
        _activity = getActivity();

        getInstrumentation().callActivityOnDestroy(_activity);
        verify(_mockPreferences, times(1)).unregisterOnSharedPreferenceChangeListener(any(MainActivity.class));
    }

    @SmallTest
    public void testRespondsToStartButtonTouchedEventStartsGPS() throws Exception {
        _activity = getActivity();

        _bus.post(new StartButtonTouchedEvent());

        boolean serviceStarted = Services.waitForServiceToStart(GPSService.class, _activity, 20000);
        assertTrue("GPSService should have been started", serviceStarted);
    }

    @SmallTest
    public void testRespondsToStopButtonTouchedEventStopsGPS() throws Exception {
        _activity = getActivity();

        Services.startServiceAndWaitForReady(GPSService.class, _activity);

        _bus.post(new StopButtonTouchedEvent());

        boolean serviceStopped = Services.waitForServiceToStop(GPSService.class, _activity, 20000);
        assertTrue ("GPSService should have been stopped", serviceStopped);
    }

    @SmallTest
    public void testRespondsToStartButtonTouchedEventStartsPebbleBikeService() throws Exception {
        _activity = getActivity();

        _bus.post(new StartButtonTouchedEvent());

        boolean serviceStarted = Services.waitForServiceToStart(PebbleService.class, _activity, 20000);
        assertTrue ("PebbleBikeService should have been started", serviceStarted);
    }

    @SmallTest
    public void testRespondsToStopButtonTouchedEventStopsPebbleBikeService() throws Exception {
        _activity = getActivity();

        Services.startServiceAndWaitForReady(PebbleService.class, _activity);

        _bus.post(new StopButtonTouchedEvent());

        boolean serviceStopped = Services.waitForServiceToStop(PebbleService.class, _activity, 20000);
        assertTrue ("PebbleBikeService should have been stopped", serviceStopped);
    }

    @SmallTest
    public void testRespondsToStartButtonTouchedEventStartsLiveTrackingService() throws Exception {
        _activity = getActivity();

        _bus.post(new StartButtonTouchedEvent());

        boolean serviceStarted = Services.waitForServiceToStart(LiveService.class, _activity, 20000);
        assertTrue ("LiveTrackingService should have been started", serviceStarted);
    }

    @SmallTest
    public void testRespondsToStopButtonTouchedEventStopsLiveTrackingService() throws Exception {
        _activity = getActivity();

        Services.startServiceAndWaitForReady(LiveService.class, _activity);

        _bus.post(new StopButtonTouchedEvent());

        boolean serviceStopped = Services.waitForServiceToStop(LiveService.class, _activity, 20000);
        assertTrue ("LiveTrackingService should have been stopped", serviceStopped);
    }

    @SmallTest
    public void testStartsActivityRecognitionServiceWhenPreferenceSet() throws Exception {
        when(_mockPreferences.getBoolean("ACTIVITY_RECOGNITION", false)).thenReturn(true);

        _activity = getActivity();

        boolean serviceStarted = Services.waitForServiceToStart(ActivityRecognitionService.class, _activity, 20000);
        assertTrue ("ActivityRecognitionService should have been started", serviceStarted);
    }

    @SmallTest
    public void testDoesNotStartActivityRecognitionServiceWhenPreferenceSet() throws Exception {
        when(_mockPreferences.getBoolean("ACTIVITY_RECOGNITION", false)).thenReturn(false);

        _activity = getActivity();

        boolean serviceStarted = Services.waitForServiceToStop(ActivityRecognitionService.class, _activity, 20000);
        assertTrue ("ActivityRecognitionService should not have been started", serviceStarted);
    }
}
