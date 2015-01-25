package com.njackson.test.activityrecognition;

import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.DetectedActivity;
import com.njackson.activityrecognition.ActivityRecognitionServiceCommand;
import com.njackson.application.modules.AndroidModule;
import com.njackson.application.modules.ForApplication;
import com.njackson.events.ActivityRecognitionCommand.ActivityRecognitionChangeState;
import com.njackson.events.ActivityRecognitionCommand.ActivityRecognitionStatus;
import com.njackson.events.ActivityRecognitionCommand.NewActivityEvent;
import com.njackson.events.base.BaseChangeState;
import com.njackson.events.base.BaseStatus;
import com.njackson.gps.IForegroundServiceStarter;
import com.njackson.test.application.TestApplication;
import com.njackson.utils.googleplay.IGooglePlayServices;
import com.njackson.utils.services.IServiceStarter;
import com.njackson.utils.time.ITimer;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by njackson on 01/01/15.
 */
public class ActivityRecognitionServiceCommandTest extends AndroidTestCase {

    @Inject Bus _bus;
    @Inject @Named("GoogleActivity") GoogleApiClient _googleApiClient;
    @Inject IServiceStarter _serviceStarter;
    @Inject SharedPreferences _sharedPreferences;

    static IGooglePlayServices _playServices;

    private ActivityRecognitionStatus _activityStatusEvent;
    private CountDownLatch _stateLatch;
    private ActivityRecognitionServiceCommand _command;
    private static ITimer _mockTimer;
    private static IForegroundServiceStarter _mockServiceStarter;
    private TestApplication _app;

    @Module(
            includes = AndroidModule.class,
            injects = ActivityRecognitionServiceCommandTest.class,
            overrides = true,
            complete = false
    )
    class TestModule {
        @Provides IGooglePlayServices providesGooglePlayServices() { return _playServices; }
        @Provides @Singleton @Named("GoogleActivity") GoogleApiClient provideActivityRecognitionClient() { return mock(GoogleApiClient.class); }
        @Provides @Singleton IServiceStarter provideServiceStarter() { return mock(IServiceStarter.class); }
        @Provides ITimer providesTimer() { return _mockTimer; }
        @Provides @Singleton SharedPreferences provideSharedPreferences() { return mock(SharedPreferences.class); };
        @Provides
        IForegroundServiceStarter providesForegroundServiceStarter() { return _mockServiceStarter; }
        @Provides @Singleton @ForApplication
        Context provideApplicationContext() {
            return getContext();
        }
    }

    @Subscribe
    public void onGPSStatusEvent(ActivityRecognitionStatus event) {
        _activityStatusEvent = event;
        _stateLatch.countDown();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());

        _playServices = mock(IGooglePlayServices.class);
        _mockTimer = mock(ITimer.class);
        _mockServiceStarter = mock(IForegroundServiceStarter.class);

        _app = new TestApplication();
        _app.setObjectGraph(ObjectGraph.create(new TestModule()));
        _app.inject(this);
        _bus.register(this);

        _stateLatch = new CountDownLatch(1);
        _command = new ActivityRecognitionServiceCommand();
    }

    @SmallTest
    public void testGooglePlayDisableMessageReceived() throws Exception {
        when(_playServices.isGooglePlayServicesAvailable(any(Context.class))).thenReturn(ConnectionResult.API_UNAVAILABLE);

        _command.execute(_app);
        _bus.post(new ActivityRecognitionChangeState(BaseChangeState.State.START));
        _stateLatch.await(1000, TimeUnit.MILLISECONDS);

        assertEquals(BaseStatus.Status.UNABLE_TO_START, _activityStatusEvent.getStatus());
        assertFalse(_activityStatusEvent.playServicesAvailable());
    }

    @SmallTest
    public void testServiceStartedMessageReceived() throws Exception {
        when(_playServices.isGooglePlayServicesAvailable(any(Context.class))).thenReturn(ConnectionResult.SUCCESS);

        _command.execute(_app);
        _bus.post(new ActivityRecognitionChangeState(BaseChangeState.State.START));
        _stateLatch.await(1000, TimeUnit.MILLISECONDS);

        assertEquals(BaseStatus.Status.STARTED, _activityStatusEvent.getStatus());
    }

    @SmallTest
    public void testServiceStoppedMessageReceived() throws Exception {
        when(_playServices.isGooglePlayServicesAvailable(any(Context.class))).thenReturn(ConnectionResult.SUCCESS);

        _command.execute(_app);
        _bus.post(new ActivityRecognitionChangeState(BaseChangeState.State.STOP));
        _stateLatch.await(1000, TimeUnit.MILLISECONDS);

        assertEquals(BaseStatus.Status.STOPPED, _activityStatusEvent.getStatus());
    }

    @SmallTest
    public void testServiceConnectsToGooglePlayOnStart() throws Exception {
        when(_playServices.isGooglePlayServicesAvailable(any(Context.class))).thenReturn(ConnectionResult.SUCCESS);

        _command.execute(_app);
        _bus.post(new ActivityRecognitionChangeState(BaseChangeState.State.START));

        verify(_googleApiClient,timeout(1000).times(1)).connect();
    }

    @SmallTest
    public void testRegistersActivityRecogntionConnectedEvent() throws Exception {
        when(_playServices.isGooglePlayServicesAvailable(any(Context.class))).thenReturn(ConnectionResult.SUCCESS);

        _command.execute(_app);
        _bus.post(new ActivityRecognitionChangeState(BaseChangeState.State.START));

        verify(_googleApiClient,timeout(1000).times(1)).registerConnectionCallbacks(any(ActivityRecognitionServiceCommand.class));
    }

    @SmallTest
    public void testRegistersActivityRecogntionFailedEvent() throws Exception {
        when(_playServices.isGooglePlayServicesAvailable(any(Context.class))).thenReturn(ConnectionResult.SUCCESS);

        _command.execute(_app);
        _bus.post(new ActivityRecognitionChangeState(BaseChangeState.State.START));

        verify(_googleApiClient,timeout(1000).times(1)).registerConnectionFailedListener(any(ActivityRecognitionServiceCommand.class));
    }

    @SmallTest
    public void testUnRegistersActivityRecogntionConnectedEvent() throws Exception {
        when(_playServices.isGooglePlayServicesAvailable(any(Context.class))).thenReturn(ConnectionResult.SUCCESS);

        _command.execute(_app);
        _bus.post(new ActivityRecognitionChangeState(BaseChangeState.State.STOP));

        verify(_googleApiClient,timeout(1000).times(1)).unregisterConnectionCallbacks(any(ActivityRecognitionServiceCommand.class));
    }

    @SmallTest
    public void testUnRegistersActivityRecogntionFailedEvent() throws Exception {
        when(_playServices.isGooglePlayServicesAvailable(any(Context.class))).thenReturn(ConnectionResult.SUCCESS);

        _command.execute(_app);
        _bus.post(new ActivityRecognitionChangeState(BaseChangeState.State.STOP));

        verify(_googleApiClient,timeout(1000).times(1)).unregisterConnectionFailedListener(any(ActivityRecognitionServiceCommand.class));
    }

    @SmallTest
    public void testRegistersActivityRecognitionUpdatesOnConnect() throws Exception {
        when(_playServices.isGooglePlayServicesAvailable(any(Context.class))).thenReturn(ConnectionResult.SUCCESS);

        _command.execute(_app);
        _bus.post(new ActivityRecognitionChangeState(BaseChangeState.State.START));

        _command.onConnected(new Bundle());

        verify(_playServices,timeout(1000).times(1)).requestActivityUpdates(any(GoogleApiClient.class), anyLong(), any(PendingIntent.class));
    }

    @SmallTest
    public void testRegistersActivityRecognitionUpdatesOnDestroy() throws Exception {
        when(_playServices.isGooglePlayServicesAvailable(any(Context.class))).thenReturn(ConnectionResult.SUCCESS);

        _command.execute(_app);
        _bus.post(new ActivityRecognitionChangeState(BaseChangeState.State.STOP));

        verify(_playServices,timeout(1000).times(1)).removeActivityUpdates(any(GoogleApiClient.class), any(PendingIntent.class));
    }

    @SmallTest
    public void testRespondsToNewActivityEventStartsLocationWhenActivityRecognitonPreferenceSet() throws Exception {
        when(_sharedPreferences.getBoolean("ACTIVITY_RECOGNITION",false)).thenReturn(true);

        _command.execute(_app);
        _bus.post(new ActivityRecognitionChangeState(BaseChangeState.State.START));
        _bus.post(new NewActivityEvent(DetectedActivity.ON_FOOT));

        verify(_serviceStarter,timeout(2000).times(1)).startLocationServices();
    }

    @SmallTest
    public void testRespondsToNewActivityEventDoesNotStartsLocationWhenActivityRecognitonPreferenceNotSet() throws Exception {
        when(_sharedPreferences.getBoolean("ACTIVITY_RECOGNITION",false)).thenReturn(false);

        _command.execute(_app);
        _bus.post(new ActivityRecognitionChangeState(BaseChangeState.State.START));
        _bus.post(new NewActivityEvent(DetectedActivity.ON_FOOT));

        verify(_serviceStarter,timeout(2000).times(0)).startLocationServices();
    }

    @SmallTest
    public void testRespondsToNewSTILLEventAndActivityRecognitionSetStartsTimer() throws Exception {
        when(_sharedPreferences.getBoolean("ACTIVITY_RECOGNITION",false)).thenReturn(true);

        _command.execute(_app);
        _bus.post(new ActivityRecognitionChangeState(BaseChangeState.State.START));
        _bus.post(new NewActivityEvent(DetectedActivity.STILL));

        verify(_mockTimer,timeout(2000).times(1)).setTimer(anyLong(),any(ActivityRecognitionServiceCommand.class));
    }

    @SmallTest
    public void testRespondsToNewSTILLAndActivityRecognitionSetDoesNotStartsTimer() throws Exception {
        when(_sharedPreferences.getBoolean("ACTIVITY_RECOGNITION",false)).thenReturn(false);

        _command.execute(_app);
        _bus.post(new ActivityRecognitionChangeState(BaseChangeState.State.START));
        _bus.post(new NewActivityEvent(DetectedActivity.STILL));

        verify(_mockTimer,timeout(2000).times(0)).setTimer(anyLong(),any(ActivityRecognitionServiceCommand.class));
    }

    @SmallTest
    public void testRespondsToNewSTILLActivityEventDoesNotStartTimerIfTimerActive() throws Exception {
        when(_sharedPreferences.getBoolean("ACTIVITY_RECOGNITION",false)).thenReturn(true);

        _command.execute(_app);
        _bus.post(new ActivityRecognitionChangeState(BaseChangeState.State.START));

        when(_mockTimer.getActive()).thenReturn(true);
        _bus.post(new NewActivityEvent(DetectedActivity.STILL));

        verify(_mockTimer,timeout(2000).times(0)).setTimer(anyLong(),any(ActivityRecognitionServiceCommand.class));
    }

    @SmallTest
    public void testCancelsTimerWhenActivityDetected() throws Exception {
        when(_sharedPreferences.getBoolean("ACTIVITY_RECOGNITION",false)).thenReturn(true);

        _command.execute(_app);
        _bus.post(new NewActivityEvent(DetectedActivity.ON_FOOT));

        verify(_mockTimer,timeout(2000).times(1)).cancel();
    }

    @SmallTest
    public void testTimeoutHandlerStopsLocation() throws Exception {
        _command.execute(_app);
        _command.handleTimeout();

        verify(_serviceStarter,timeout(2000).times(1)).stopLocationServices();
    }

}
