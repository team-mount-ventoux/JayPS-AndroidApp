package com.njackson.test.activities;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.njackson.activities.MainActivity;
import com.njackson.analytics.IAnalytics;
import com.njackson.application.modules.AndroidModule;
import com.njackson.changelog.IChangeLog;
import com.njackson.changelog.IChangeLogBuilder;
import com.njackson.events.GPSServiceCommand.ResetGPSState;
import com.njackson.events.UI.StartButtonTouchedEvent;
import com.njackson.events.UI.StopButtonTouchedEvent;
import com.njackson.events.GoogleFitCommand.GoogleFitStatus;
import com.njackson.events.base.BaseStatus;
import com.njackson.test.application.TestApplication;
import com.njackson.utils.googleplay.IGooglePlayServices;
import com.njackson.utils.services.IServiceStarter;
import com.njackson.pebble.IMessageManager;
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
    private static IChangeLogBuilder _mockChangeLogBuilder;
    private AlertDialog _mockAlertDialog;
    private IChangeLog _mockChangeLog;

    @Module(
            includes = AndroidModule.class,
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

        @Provides
        IChangeLogBuilder providesChangeLogBuilder() { return _mockChangeLogBuilder; }
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

        _mockChangeLogBuilder = mock(IChangeLogBuilder.class);
        _mockChangeLog = mock(IChangeLog.class);
        _mockAlertDialog = mock(AlertDialog.class);

        when(_mockChangeLog.getDialog()).thenReturn(_mockAlertDialog);
        when(_mockChangeLogBuilder.setActivity(any(MainActivity.class))).thenReturn(_mockChangeLogBuilder);
        when(_mockChangeLogBuilder.build()).thenReturn(_mockChangeLog);
    }

    private ConnectionResult createConnectionResult() {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getInstrumentation().getTargetContext(),1, new Intent("MOCK"),0);
        ConnectionResult result = new ConnectionResult(0,pendingIntent);
        return  result;
    }

    @SmallTest
    public void testSendsTrackAppOpenedAnalyticsOnCreate() {
        _activity = getActivity();

        verify(_mockAnalytics, times(1)).trackAppOpened(any(Intent.class));
    }

    @SmallTest
    public void testRegistersForSharedPreferencesUpdatesOnCreate() {
        _activity = getActivity();

        verify(_mockPreferences,times(1)).registerOnSharedPreferenceChangeListener(any(MainActivity.class));
    }

    @SmallTest
    public void testStartsActivityServiceOnCreateWhenActivityRecognitionPreferenceSet() {
        when(_mockPreferences.getBoolean("ACTIVITY_RECOGNITION",false)).thenReturn(true);
        _activity = getActivity();

        verify(_mockServiceStarter, times(1)).startActivityService();
    }

    @SmallTest
    public void testDoesNotStartsActivityServiceOnCreateWhenActivityRecognitionPreferenceNotSet() {
        when(_mockPreferences.getBoolean("ACTIVITY_RECOGNITION",false)).thenReturn(false);
        _activity = getActivity();

        verify(_mockServiceStarter,times(0)).startActivityService();
    }
/* OK on a real phone, but fails too often on Travis
    @SmallTest
    public void testUnRegistersForSharedPreferencesUpdatesOnDestroy() throws InterruptedException {
        _activity = getActivity();
        _activity.finish();

        verify(_mockPreferences, timeout(2000).times(1)).unregisterOnSharedPreferenceChangeListener(any(MainActivity.class));
    }
*/
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

        _bus.post(new GoogleFitStatus(BaseStatus.Status.STARTED, createConnectionResult()));

        verify(_mockPlayServices,timeout(2000).times(0)).connectionResultHasResolution(any(ConnectionResult.class));
        verify(_mockPlayServices,timeout(2000).times(0)).startConnectionResultResolution(any(ConnectionResult.class), any(MainActivity.class));
    }

    @SmallTest
    public void testOnGoogleFitConnectionFailedWithNoResolutionShowsErrorDialog() {
        when(_mockPlayServices.connectionResultHasResolution(any(ConnectionResult.class))).thenReturn(false);

        _activity = getActivity();

        _bus.post(new GoogleFitStatus(BaseStatus.Status.UNABLE_TO_START, createConnectionResult()));

        verify(_mockPlayServices,timeout(2000).times(1)).showConnectionResultErrorDialog(any(ConnectionResult.class), any(MainActivity.class));
    }

    @SmallTest
    public void testOnGoogleFitConnectionFailedWitResolutionDoesNotShowErrorDialog() {
        when(_mockPlayServices.connectionResultHasResolution(any(ConnectionResult.class))).thenReturn(true);

        _activity = getActivity();

        _bus.post(new GoogleFitStatus(BaseStatus.Status.UNABLE_TO_START, createConnectionResult()));

        verify(_mockPlayServices,timeout(2000).times(0)).showConnectionResultErrorDialog(any(ConnectionResult.class), any(MainActivity.class));
    }

    @SmallTest
    public void testOnGoogleFitConnectionFailedWithResolutionStartsResultResolution() throws IntentSender.SendIntentException {
        when(_mockPlayServices.connectionResultHasResolution(null)).thenReturn(true);

        _activity = getActivity();

        _bus.post(new GoogleFitStatus(BaseStatus.Status.UNABLE_TO_START));

        verify(_mockPlayServices,timeout(2000).times(1)).startConnectionResultResolution(any(ConnectionResult.class), any(MainActivity.class));
    }

    @SmallTest
    public void testOnGoogleFitConnectionFailedWithResolutionReceivedSecondTimeDoesNotStartsResultResolution() throws IntentSender.SendIntentException {
        when(_mockPlayServices.connectionResultHasResolution(null)).thenReturn(true);

        _activity = getActivity();

        _bus.post(new GoogleFitStatus(BaseStatus.Status.UNABLE_TO_START));
        _bus.post(new GoogleFitStatus(BaseStatus.Status.UNABLE_TO_START));

        verify(_mockPlayServices,timeout(2000).times(1)).startConnectionResultResolution(any(ConnectionResult.class), any(MainActivity.class));
    }

    @SmallTest
    public void testStopsActivityRecognitionServiceWhenACTIVITY_RECOGNITIONPreferenceChanged() throws Exception {
        when(_mockPreferences.getBoolean("ACTIVITY_RECOGNITION", false)).thenReturn(true);

        _activity = getActivity();

        when(_mockPreferences.getBoolean("ACTIVITY_RECOGNITION", false)).thenReturn(false);

        _activity.onSharedPreferenceChanged(_mockPreferences, "ACTIVITY_RECOGNITION");

        verify(_mockServiceStarter,times(1)).stopActivityService();
    }

    @SmallTest
    public void testStartsActivityRecognitionServiceWhenACTIVITY_RECOGNITIONPreferenceChanged() throws Exception {
        when(_mockPreferences.getBoolean("ACTIVITY_RECOGNITION", false)).thenReturn(false);

        _activity = getActivity();

        when(_mockPreferences.getBoolean("ACTIVITY_RECOGNITION", false)).thenReturn(true);

        _activity.onSharedPreferenceChanged(_mockPreferences, "ACTIVITY_RECOGNITION");

        verify(_mockServiceStarter,times(1)).startActivityService();
    }
/* Disable Google Fit for v2 (shifted to v2.1+)
    @SmallTest
    public void testStopsActivityRecognitionServiceWhenGOOGLE_FITPreferenceChanged() throws Exception {
        when(_mockPreferences.getBoolean("GOOGLE_FIT", false)).thenReturn(true);

        _activity = getActivity();

        when(_mockPreferences.getBoolean("GOOGLE_FIT", false)).thenReturn(false);

        _activity.onSharedPreferenceChanged(_mockPreferences,"GOOGLE_FIT");

        verify(_mockServiceStarter,times(1)).stopActivityService();
    }
*/
/* Disable Google Fit for v2 (shifted to v2.1+)
    @SmallTest
    public void testStartsActivityRecognitionServiceWhenGOOGLE_FITPreferenceChanged() throws Exception {
        when(_mockPreferences.getBoolean("GOOGLE_FIT", false)).thenReturn(false);

        _activity = getActivity();

        when(_mockPreferences.getBoolean("GOOGLE_FIT", false)).thenReturn(true);

        _activity.onSharedPreferenceChanged(_mockPreferences,"GOOGLE_FIT");

        verify(_mockServiceStarter,times(1)).startActivityService();
    }
*/
    @SmallTest
    public void testDoesNothingWhenIncorrectPreferenceChanged() throws Exception {
        _activity = getActivity();
        _activity.onSharedPreferenceChanged(_mockPreferences,"NONSENSE_PREFERENCE");

        verify(_mockServiceStarter,times(0)).startActivityService();
        verify(_mockServiceStarter,times(0)).stopActivityService();
    }

    @SmallTest
    public void testSetsActivityToChangeLogBuilderOnCreate() throws Exception {
        _activity = getActivity();

        verify(_mockChangeLogBuilder,times(1)).setActivity(any(MainActivity.class));
    }

    @SmallTest
    public void testCallsBuildOnChangeLogBuilderOnCreate() throws Exception {
        _activity = getActivity();

        verify(_mockChangeLogBuilder,times(1)).build();
    }

    @SmallTest
    public void testShowsChangeLogWhenChangesOnCreate() throws Exception {
        when(_mockChangeLog.isFirstRun()).thenReturn(true);

        _activity = getActivity();

        verify(_mockAlertDialog,times(1)).show();
    }

    @SmallTest
    public void testDoesNOTShowChangeLogWhenNOChangesOnCreate() throws Exception {
        when(_mockChangeLog.isFirstRun()).thenReturn(false);

        _activity = getActivity();

        verify(_mockAlertDialog,times(0)).show();
    }
}
