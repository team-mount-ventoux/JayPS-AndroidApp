package com.njackson.test.fit;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.RecordingApi;
import com.google.android.gms.fitness.SessionsApi;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Session;
import com.njackson.application.modules.PebbleBikeModule;
import com.njackson.events.status.GoogleFitStatus;
import com.njackson.fit.GoogleFitService;
import com.njackson.gps.GPSService;
import com.njackson.test.application.TestApplication;
import com.njackson.utils.googleplay.GooglePlayServices;
import com.njackson.utils.googleplay.IGoogleFitSessionManager;
import com.njackson.utils.googleplay.IGooglePlayServices;
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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by njackson on 05/01/15.
 */
public class GoogleFitServiceTest extends ServiceTestCase<GoogleFitService> {
    private GoogleFitService _service;
    private CountDownLatch _stateLatch;

    @Inject Bus _bus;
    @Inject @Named("GoogleFit") GoogleApiClient _googleAPIClient;

    private GoogleFitStatus _state;
    private static IGoogleFitSessionManager _mockSessionManager;

    @Module(
            includes = PebbleBikeModule.class,
            injects = GoogleFitServiceTest.class,
            overrides = true,
            complete = false
    )
    static class TestModule {

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

    /**
     * Constructor
     *
     * @param serviceClass The type of the service under test.
     */
    public GoogleFitServiceTest(Class<GoogleFitService> serviceClass) {
        super(serviceClass);
    }

    public GoogleFitServiceTest() {
        super(GoogleFitService.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        System.setProperty("dexmaker.dexcache", getSystemContext().getCacheDir().getPath());

        setupMocks();

        TestApplication app = new TestApplication();
        app.setObjectGraph(ObjectGraph.create(TestModule.class));
        app.inject(this);
        _bus.register(this);

        setApplication(app);

        _stateLatch = new CountDownLatch(1);
    }

    private void setupMocks() {
        _mockSessionManager = mock(IGoogleFitSessionManager.class);
    }

    private void startService() throws Exception {
        Intent startIntent = new Intent(getSystemContext(), GPSService.class);
        startService(startIntent);
        _service = getService();
        _stateLatch.await(2000, TimeUnit.MILLISECONDS);
    }

    @SmallTest
    public void testOnBindReturnsNull() throws Exception {
        startService();
        IBinder binder = _service.onBind(new Intent());

        assertNull(binder);
    }

    @SmallTest
    public void testOnStartSendsServiceStarted() throws Exception {
        startService();

        assertEquals(GoogleFitStatus.State.SERVICE_STARTED,_state.getState());
    }

    @SmallTest
    public void testOnStartedConnectsToGoogleFit() throws Exception {
        startService();

        verify(_googleAPIClient,timeout(2000).times(1)).connect();
    }

    @SmallTest
    public void testOnStartRegistersGoogleClientConnectedHandlers() throws Exception {
        startService();

        verify(_googleAPIClient,timeout(2000).times(1)).registerConnectionCallbacks(any(GoogleFitService.class));
    }

    @SmallTest
    public void testOnStartRegistersGoogleClientConnectionFailedHandlers() throws Exception {
        startService();

        verify(_googleAPIClient,timeout(2000).times(1)).registerConnectionFailedListener(any(GoogleFitService.class));
    }

    @SmallTest
    public void testOnDestroyRemovesGoogleClientConnectedHandlers() throws Exception {
        startService();
        shutdownService();

        verify(_googleAPIClient,timeout(2000).times(1)).unregisterConnectionCallbacks(any(GoogleFitService.class));
    }

    @SmallTest
    public void testOnDestroyRemovesGoogleClientConnectionFailedHandlers() throws Exception {
        startService();
        shutdownService();

        verify(_googleAPIClient,timeout(2000).times(1)).unregisterConnectionFailedListener(any(GoogleFitService.class));
    }

    @SmallTest
    public void testOnDestroyDoesNotStopsSessionWhenNotConnected() throws Exception {
        when(_googleAPIClient.isConnected()).thenReturn(false);

        startService();
        shutdownService();

        verify(_mockSessionManager,timeout(2000).times(0)).saveActiveSession(anyLong());
    }

    @SmallTest
    public void testOnConnectionFailedPostsEvent() throws Exception {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(),1, new Intent("MOCK"),0);
        ConnectionResult result = new ConnectionResult(0,pendingIntent);

        startService();
        _stateLatch = new CountDownLatch(1);
        _service.onConnectionFailed(result);

        _stateLatch.await(2000,TimeUnit.MILLISECONDS);

        assertEquals(GoogleFitStatus.State.GOOGLEFIT_CONNECTION_FAILED, _state.getState());
    }

    @SmallTest
    public void testOnConnectedStartsSessionManager() throws Exception {
        startService();

        _service.onConnected(new Bundle());

        verify(_mockSessionManager, timeout(2000).times(1)).startSession(anyLong(),any(GoogleApiClient.class));
    }
}
