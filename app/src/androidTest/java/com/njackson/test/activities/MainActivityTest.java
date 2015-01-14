package com.njackson.test.activities;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.njackson.activities.MainActivity;
import com.njackson.analytics.IAnalytics;
import com.njackson.application.modules.PebbleBikeModule;
import com.njackson.events.GPSService.ResetGPSState;
import com.njackson.events.UI.StartButtonTouchedEvent;
import com.njackson.events.UI.StopButtonTouchedEvent;
import com.njackson.events.status.GoogleFitStatus;
import com.njackson.test.application.TestApplication;
import com.njackson.utils.googleplay.IGooglePlayServices;
import com.njackson.utils.services.IServiceStarter;
import com.njackson.virtualpebble.IMessageManager;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by server on 30/03/2014.
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private static final String TAG = "PB-MainActivityTest";

    @Inject Bus _bus;
    @Inject SharedPreferences _mockPreferences;
    @Inject IServiceStarter _mockServiceStarter;
    @Inject IAnalytics _mockAnalytics;

    private MainActivity _activity;
    private TestApplication _app;

    private SharedPreferences.Editor _mockEditor;
    private static IGooglePlayServices _mockPlayServices;

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

        @Provides @Singleton @Named("GoogleActivity")
        GoogleApiClient provideActivityRecognitionClient() { return mock(GoogleApiClient.class); }

        @Provides
        IGooglePlayServices providesGooglePlayServices() { return _mockPlayServices; }

        @Provides @Singleton
        IServiceStarter provideServiceStarter() { return mock(IServiceStarter.class); }
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

        Log.d(TAG, "Setup Complete");
    }

    private void setupMocks() {
        _mockEditor = mock(SharedPreferences.Editor.class, RETURNS_DEEP_STUBS);
        _mockPlayServices = mock(IGooglePlayServices.class);
        when(_mockPreferences.edit()).thenReturn(_mockEditor);
    }

    private ConnectionResult createConnectionResult() {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getInstrumentation().getTargetContext(),1, new Intent("MOCK"),0);
        ConnectionResult result = new ConnectionResult(0,pendingIntent);
        return  result;
    }

    @SmallTest
    public void testSendsTrackAppOpenedAnalyticsOnCreate() {
        _activity = getActivity();

        verify(_mockAnalytics,times(1)).trackAppOpened(any(Intent.class));
    }

    @SmallTest
    public void testRespondsToStartButtonTouchedEventStartsServices() throws Exception {
        _activity = getActivity();

        _bus.post(new StartButtonTouchedEvent());

        verify(_mockServiceStarter, timeout(2000).times(1)).startLocationServices();
    }

    @SmallTest
    public void testRespondsToStopButtonTouchedEventStopsServices() throws Exception {
        _activity = getActivity();

        _bus.post(new StopButtonTouchedEvent());

        verify(_mockServiceStarter, timeout(2000).times(1)).stopLocationServices();
    }

    @SmallTest
    public void testOnOtherGoogleFitEventsDoesNothing() throws IntentSender.SendIntentException {
        when(_mockPlayServices.connectionResultHasResolution(any(ConnectionResult.class))).thenReturn(false);

        _activity = getActivity();

        _bus.post(new GoogleFitStatus(GoogleFitStatus.State.GOOGLEFIT_CONNECTED, createConnectionResult()));

        verify(_mockPlayServices,timeout(2000).times(0)).connectionResultHasResolution(any(ConnectionResult.class));
        verify(_mockPlayServices,timeout(2000).times(0)).startConnectionResultResolution(any(ConnectionResult.class), any(MainActivity.class));
    }

    @SmallTest
    public void testOnGoogleFitConnectionFailedWithNoResolutionShowsErrorDialog() {
        when(_mockPlayServices.connectionResultHasResolution(any(ConnectionResult.class))).thenReturn(false);

        _activity = getActivity();

        _bus.post(new GoogleFitStatus(GoogleFitStatus.State.GOOGLEFIT_CONNECTION_FAILED, createConnectionResult()));

        verify(_mockPlayServices,timeout(2000).times(1)).showConnectionResultErrorDialog(any(ConnectionResult.class), any(MainActivity.class));
    }

    @SmallTest
    public void testOnGoogleFitConnectionFailedWitResolutionDoesNotShowErrorDialog() {
        when(_mockPlayServices.connectionResultHasResolution(any(ConnectionResult.class))).thenReturn(true);

        _activity = getActivity();

        _bus.post(new GoogleFitStatus(GoogleFitStatus.State.GOOGLEFIT_CONNECTION_FAILED, createConnectionResult()));

        verify(_mockPlayServices,timeout(2000).times(0)).showConnectionResultErrorDialog(any(ConnectionResult.class),any(MainActivity.class));
    }

    @SmallTest
    public void testOnGoogleFitConnectionFailedWithResolutionStartsResultResolution() throws IntentSender.SendIntentException {
        when(_mockPlayServices.connectionResultHasResolution(null)).thenReturn(true);

        _activity = getActivity();

        _bus.post(new GoogleFitStatus(GoogleFitStatus.State.GOOGLEFIT_CONNECTION_FAILED));

        verify(_mockPlayServices,timeout(2000).times(1)).startConnectionResultResolution(any(ConnectionResult.class), any(MainActivity.class));
    }

    @SmallTest
    public void testOnGoogleFitConnectionFailedWithResolutionReceivedSecondTimeDoesNotStartsResultResolution() throws IntentSender.SendIntentException {
        when(_mockPlayServices.connectionResultHasResolution(null)).thenReturn(true);

        _activity = getActivity();

        _bus.post(new GoogleFitStatus(GoogleFitStatus.State.GOOGLEFIT_CONNECTION_FAILED));
        _bus.post(new GoogleFitStatus(GoogleFitStatus.State.GOOGLEFIT_CONNECTION_FAILED));

        verify(_mockPlayServices,timeout(2000).times(1)).startConnectionResultResolution(any(ConnectionResult.class), any(MainActivity.class));
    }
}
