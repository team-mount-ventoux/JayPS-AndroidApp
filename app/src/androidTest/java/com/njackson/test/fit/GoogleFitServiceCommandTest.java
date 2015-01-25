package com.njackson.test.fit;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.DetectedActivity;
import com.njackson.application.modules.AndroidModule;
import com.njackson.events.ActivityRecognitionCommand.NewActivityEvent;
import com.njackson.events.GoogleFitCommand.GoogleFitChangeState;
import com.njackson.events.GoogleFitCommand.GoogleFitStatus;
import com.njackson.events.base.BaseChangeState;
import com.njackson.events.base.BaseStatus;
import com.njackson.fit.GoogleFitServiceCommand;
import com.njackson.test.application.TestApplication;
import com.njackson.utils.googleplay.IGoogleFitSessionManager;
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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by njackson on 05/01/15.
 */
public class GoogleFitServiceCommandTest extends AndroidTestCase {
    private CountDownLatch _stateLatch;

    @Inject Bus _bus;
    @Inject @Named("GoogleFit") GoogleApiClient _googleAPIClient;

    private GoogleFitStatus _state;
    private static IGoogleFitSessionManager _mockSessionManager;
    private TestApplication _app;
    private GoogleFitServiceCommand _command;

    @Module(
            includes = AndroidModule.class,
            injects = GoogleFitServiceCommandTest.class,
            overrides = true,
            complete = false
    )
    class TestModule {

        @Provides
        @Singleton
        @Named("GoogleFit")
        GoogleApiClient provideFitnessAPIClient() { return mock(GoogleApiClient.class); }

        @Provides
        IGoogleFitSessionManager providesGoogleFitSessionManager() { return _mockSessionManager; }
    }


    @Subscribe
    public void onChangeStateEvent(GoogleFitStatus state) {
        _state = state;
        _stateLatch.countDown();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());

        setupMocks();

        _app = new TestApplication();
        _app.setObjectGraph(ObjectGraph.create(new TestModule()));
        _app.inject(this);
        _bus.register(this);

        _stateLatch = new CountDownLatch(1);
        _command = new GoogleFitServiceCommand();
    }

    private void setupMocks() {
        _mockSessionManager = mock(IGoogleFitSessionManager.class);
    }

    @SmallTest
    public void testOnStartSendsServiceStarted() throws Exception {
        _command.execute(_app);
        _bus.post(new GoogleFitChangeState(BaseChangeState.State.START));
        _stateLatch.await(1000, TimeUnit.MILLISECONDS);

        assertEquals(BaseStatus.Status.STARTED, _state.getStatus());
    }

    @SmallTest
    public void testOnStopSendsServiceStopped() throws Exception {
        _command.execute(_app);
        _bus.post(new GoogleFitChangeState(BaseChangeState.State.STOP));
        _stateLatch.await(1000, TimeUnit.MILLISECONDS);

        assertEquals(BaseStatus.Status.STOPPED, _state.getStatus());
    }

    @SmallTest
    public void testOnStartedConnectsToGoogleFit() throws Exception {
        _command.execute(_app);
        _bus.post(new GoogleFitChangeState(BaseChangeState.State.START));

        verify(_googleAPIClient,timeout(2000).times(1)).connect();
    }

    @SmallTest
    public void testOnStartRegistersGoogleClientConnectedHandlers() throws Exception {
        _command.execute(_app);
        _bus.post(new GoogleFitChangeState(BaseChangeState.State.START));

        verify(_googleAPIClient,timeout(2000).times(1)).registerConnectionCallbacks(any(GoogleFitServiceCommand.class));
    }

    @SmallTest
    public void testOnStartRegistersGoogleClientConnectionFailedHandlers() throws Exception {
        _command.execute(_app);
        _bus.post(new GoogleFitChangeState(BaseChangeState.State.START));

        verify(_googleAPIClient,timeout(2000).times(1)).registerConnectionFailedListener(any(GoogleFitServiceCommand.class));
    }

    @SmallTest
    public void testOnDestroyRemovesGoogleClientConnectedHandlers() throws Exception {
        _command.execute(_app);
        _bus.post(new GoogleFitChangeState(BaseChangeState.State.STOP));

        verify(_googleAPIClient,timeout(2000).times(1)).unregisterConnectionCallbacks(any(GoogleFitServiceCommand.class));
    }

    @SmallTest
    public void testOnDestroyRemovesGoogleClientConnectionFailedHandlers() throws Exception {
        _command.execute(_app);
        _bus.post(new GoogleFitChangeState(BaseChangeState.State.STOP));

        verify(_googleAPIClient,timeout(2000).times(1)).unregisterConnectionFailedListener(any(GoogleFitServiceCommand.class));
    }

    @SmallTest
    public void testOnDestroyStopsSessionWhenConnected() throws Exception {
        when(_googleAPIClient.isConnected()).thenReturn(true);

        _command.execute(_app);
        _bus.post(new GoogleFitChangeState(BaseChangeState.State.STOP));

        verify(_mockSessionManager,timeout(2000).times(1)).saveActiveSession(anyLong());
    }

    @SmallTest
    public void testOnDestroyDoesNotStopsSessionWhenNotConnected() throws Exception {
        when(_googleAPIClient.isConnected()).thenReturn(false);

        _command.execute(_app);
        _bus.post(new GoogleFitChangeState(BaseChangeState.State.STOP));

        verify(_mockSessionManager,timeout(2000).times(0)).saveActiveSession(anyLong());
    }

    @SmallTest
    public void testOnConnectionFailedPostsEvent() throws Exception {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(),1, new Intent("MOCK"),0);
        ConnectionResult result = new ConnectionResult(0,pendingIntent);

        _command.execute(_app);
        _bus.post(new GoogleFitChangeState(BaseChangeState.State.START));
        _stateLatch.await(1000, TimeUnit.MILLISECONDS);

        _stateLatch = new CountDownLatch(1);
        _command.onConnectionFailed(result);

        _stateLatch.await(2000,TimeUnit.MILLISECONDS);

        assertEquals(BaseStatus.Status.UNABLE_TO_START, _state.getStatus());
    }

    @SmallTest
    public void testOnConnectedStartsSessionManager() throws Exception {
        _command.execute(_app);

        _command.onConnected(new Bundle());

        verify(_mockSessionManager, timeout(2000).times(1)).startSession(anyLong(),any(GoogleApiClient.class));
    }

    @SmallTest
    public void testOnNewActivityRunningCreatesDataPoint() throws Exception {
        _command.execute(_app);
        _bus.post(new GoogleFitChangeState(BaseChangeState.State.START));

        _command.onConnected(new Bundle());
        _bus.post(new NewActivityEvent(DetectedActivity.RUNNING));

        verify(_mockSessionManager, timeout(2000).times(1)).addDataPoint(anyLong(),anyInt());
    }

    @SmallTest
    public void testOnNewActivityBikingCreatesDataPoint() throws Exception {
        _command.execute(_app);
        _bus.post(new GoogleFitChangeState(BaseChangeState.State.START));

        _command.onConnected(new Bundle());
        _bus.post(new NewActivityEvent(DetectedActivity.ON_BICYCLE));

        verify(_mockSessionManager, timeout(2000).times(1)).addDataPoint(anyLong(),anyInt());
    }

    @SmallTest
    public void testOnNewActivityWalkingCreatesDataPoint() throws Exception {
        _command.execute(_app);
        _bus.post(new GoogleFitChangeState(BaseChangeState.State.START));

        _command.onConnected(new Bundle());
        _bus.post(new NewActivityEvent(DetectedActivity.WALKING));

        verify(_mockSessionManager, timeout(2000).times(1)).addDataPoint(anyLong(),anyInt());
    }

    @SmallTest
    public void testOnNewActivityStillDoesNotCreateDataPoint() throws Exception {
        _command.execute(_app);
        _bus.post(new GoogleFitChangeState(BaseChangeState.State.START));

        _command.onConnected(new Bundle());
        _bus.post(new NewActivityEvent(DetectedActivity.STILL));

        verify(_mockSessionManager, timeout(2000).times(0)).addDataPoint(anyLong(),anyInt());
    }

    @SmallTest
    public void testOnNewActivityOnlyAddsDataPointWhenStarted() throws Exception {
        _command.execute(_app);
        _bus.post(new GoogleFitChangeState(BaseChangeState.State.STOP));

        _command.onConnected(new Bundle());
        _bus.post(new NewActivityEvent(DetectedActivity.STILL));

        verify(_mockSessionManager, timeout(2000).times(0)).addDataPoint(anyLong(),anyInt());
    }
}
