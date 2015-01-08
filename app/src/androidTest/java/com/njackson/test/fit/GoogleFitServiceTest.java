package com.njackson.test.fit;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.RecordingApi;
import com.google.android.gms.fitness.data.DataType;
import com.njackson.application.modules.PebbleBikeModule;
import com.njackson.events.status.GoogleFitStatus;
import com.njackson.fit.GoogleFitService;
import com.njackson.gps.GPSService;
import com.njackson.test.application.TestApplication;
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
    private static RecordingApi _mockRecordingApi;
    private PendingResult<com.google.android.gms.common.api.Status> _pendingResultMock;

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
        RecordingApi providesGoogleFitRecordingApi() { return _mockRecordingApi; }
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

        _mockRecordingApi = mock(RecordingApi.class);
        _pendingResultMock = mock(PendingResult.class);
        when(_mockRecordingApi.subscribe(any(GoogleApiClient.class),any(com.google.android.gms.fitness.data.DataType.class)))
                .thenReturn(_pendingResultMock);

        TestApplication app = new TestApplication();
        app.setObjectGraph(ObjectGraph.create(TestModule.class));
        app.inject(this);
        _bus.register(this);

        setApplication(app);

        _stateLatch = new CountDownLatch(1);
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
    public void testOnStartRemovesGoogleClientConnectionFailedHandlers() throws Exception {
        startService();
        shutdownService();

        verify(_googleAPIClient,timeout(2000).times(1)).unregisterConnectionFailedListener(any(GoogleFitService.class));
    }

    @SmallTest
    public void testOnConnectedStartsRecordingAPI() throws Exception {
        startService();
        _service.onConnected(new Bundle());

        verify(_mockRecordingApi, timeout(2000).times(1)).subscribe(_googleAPIClient, DataType.TYPE_DISTANCE_DELTA);
    }

    @SmallTest
    public void testOnConnectedRegistersCallbackToRecordingApi() throws Exception {
        startService();
        _service.onConnected(new Bundle());

        verify(_pendingResultMock,timeout(2000).times(1)).setResultCallback(any(ResultCallback.class));
    }

    @SmallTest
    public void testDestroyedStopsRecordingAPI() throws Exception {
        startService();
        shutdownService();

        verify(_mockRecordingApi, timeout(2000).times(1)).unsubscribe(_googleAPIClient, DataType.TYPE_DISTANCE_DELTA);
    }
}
