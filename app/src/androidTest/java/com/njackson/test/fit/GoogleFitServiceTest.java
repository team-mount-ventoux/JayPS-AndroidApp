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
    private static RecordingApi _mockRecordingApi;
    private static SessionsApi _mockSessionsApi;
    private PendingResult<com.google.android.gms.common.api.Status> _pendingResultMock;
    private static Session.Builder _mockSessionBuilder;
    private static IGooglePlayServices _mockPlayServices;

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

        @Provides
        SessionsApi providesGoogleFitSessionsApi() { return _mockSessionsApi; }

        @Provides
        IGooglePlayServices providesGooglePlayServices() { return _mockPlayServices; }
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
        _mockRecordingApi = mock(RecordingApi.class);
        _pendingResultMock = mock(PendingResult.class);
        when(_mockRecordingApi.subscribe(any(GoogleApiClient.class),any(DataType.class)))
                .thenReturn(_pendingResultMock);

        _mockSessionsApi = mock(SessionsApi.class);

        _mockSessionBuilder = mock(Session.Builder.class);
        when(_mockSessionBuilder.setName(anyString())).thenReturn(_mockSessionBuilder);
        when(_mockSessionBuilder.setIdentifier(anyString())).thenReturn(_mockSessionBuilder);
        when(_mockSessionBuilder.setStartTime(anyLong(),any(TimeUnit.class))).thenReturn(_mockSessionBuilder);

        _mockPlayServices = mock(IGooglePlayServices.class);
        when(_mockPlayServices.newSessionBuilder()).thenReturn(_mockSessionBuilder);
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
    public void testDestroyedStopsRecordingAPIWhenConnected() throws Exception {
        when(_googleAPIClient.isConnected()).thenReturn(true);

        startService();
        shutdownService();

        verify(_mockRecordingApi, timeout(2000).times(1)).unsubscribe(_googleAPIClient, DataType.TYPE_DISTANCE_DELTA);
    }

    @SmallTest
    public void testDestroyedDoesNotStopsRecordingAPIWhenNotConnected() throws Exception {
        when(_googleAPIClient.isConnected()).thenReturn(false);

        startService();
        shutdownService();

        verify(_mockRecordingApi, timeout(2000).times(0)).unsubscribe(_googleAPIClient, DataType.TYPE_DISTANCE_DELTA);
    }

    @SmallTest
    public void testOnConnectedCreatesNewSessionBuilder() throws Exception {
        startService();
        _service.onConnected(new Bundle());

        verify(_mockPlayServices,timeout(2000).times(1)).newSessionBuilder();
    }

    @SmallTest
    public void testOnConnectedSetSessionBuilderSessionIdentifier() throws Exception {
        when(_mockPlayServices.generateSessionIdentifier(anyLong())).thenReturn("MockSessionIdentifier");

        startService();
        _service.onConnected(new Bundle());

        verify(_mockSessionBuilder,timeout(2000).times(1)).setIdentifier("MockSessionIdentifier");
    }

    @SmallTest
    public void testOnConnectedSetSessionBuilderSessionName() throws Exception {
        when(_mockPlayServices.generateSessionName()).thenReturn("MockSessionName");

        startService();
        _service.onConnected(new Bundle());

        verify(_mockSessionBuilder,timeout(2000).times(1)).setName("MockSessionName");
    }

    @SmallTest
    public void testOnConnectedSetSessionBuilderStartTime() throws Exception {
        startService();
        _service.onConnected(new Bundle());

        verify(_mockSessionBuilder,timeout(2000).times(1)).setStartTime(anyLong(),any(TimeUnit.class));
    }

    @SmallTest
    public void testOnConnectedSetsSession() throws Exception {
        startService();
        _service.onConnected(new Bundle());

        verify(_mockSessionsApi,timeout(2000).times(1)).startSession(any(GoogleApiClient.class),any(Session.class));
    }

    @SmallTest
    public void testOnDestroyStopsSessionWhenConnected() throws Exception {
        when(_googleAPIClient.isConnected()).thenReturn(true);

        when(_mockPlayServices.generateSessionIdentifier(anyLong())).thenReturn("MockSessionIdentifier");
        startService();
        _service.onConnected(new Bundle());
        shutdownService();

        verify(_mockSessionsApi,timeout(2000).times(1)).stopSession(_googleAPIClient,"MockSessionIdentifier");
    }

    @SmallTest
    public void testOnDestroyDoesNotStopsSessionWhenNotConnected() throws Exception {
        when(_googleAPIClient.isConnected()).thenReturn(false);

        when(_mockPlayServices.generateSessionIdentifier(anyLong())).thenReturn("MockSessionIdentifier");
        startService();
        shutdownService();

        verify(_mockSessionsApi,timeout(2000).times(0)).stopSession(_googleAPIClient,"MockSessionIdentifier");
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
}
