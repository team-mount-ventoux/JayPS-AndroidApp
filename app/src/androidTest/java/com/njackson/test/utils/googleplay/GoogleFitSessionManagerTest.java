package com.njackson.test.utils.googleplay;

import android.content.Context;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.SessionsApi;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.result.SessionStopResult;
import com.google.android.gms.location.DetectedActivity;
import com.njackson.utils.googleplay.GoogleFitSessionManager;
import com.njackson.utils.googleplay.IGooglePlayServices;

import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by njackson on 11/01/15.
 */
public class GoogleFitSessionManagerTest extends AndroidTestCase {

    IGooglePlayServices _playServices;
    private SessionsApi _mockSessionsApi;
    private Session.Builder _mockSessionBuilder;
    private IGooglePlayServices _mockPlayServices;
    private GoogleFitSessionManager _sessionManager;
    private GoogleApiClient _mockGoogleApiClient;
    private Context _mockContext;
    private PendingResult<SessionStopResult> _mockResult;
    private Session _mockSession;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());

        setupMocks();

        _sessionManager = new GoogleFitSessionManager(_mockContext,_mockPlayServices, _mockSessionsApi);
    }

    private void setupMocks() {

        _mockContext = mock(Context.class);
        when(_mockContext.getPackageName()).thenReturn("com.somepackage.or.other");

        _mockGoogleApiClient = mock(GoogleApiClient.class);
        _mockSessionsApi = mock(SessionsApi.class);

        _mockSession = mock(Session.class);

        _mockSessionBuilder = mock(Session.Builder.class);
        when(_mockSessionBuilder.setName(anyString())).thenReturn(_mockSessionBuilder);
        when(_mockSessionBuilder.setIdentifier(anyString())).thenReturn(_mockSessionBuilder);
        when(_mockSessionBuilder.setStartTime(anyLong(),any(TimeUnit.class))).thenReturn(_mockSessionBuilder);
        when(_mockSessionBuilder.build()).thenReturn(_mockSession);

        _mockPlayServices = mock(IGooglePlayServices.class);
        when(_mockPlayServices.newSessionBuilder()).thenReturn(_mockSessionBuilder);

        _mockResult = mock(PendingResult.class);
        when(_mockSessionsApi.stopSession(any(GoogleApiClient.class),anyString())).thenReturn(_mockResult);
    }

    @SmallTest
    public void testConstructoreSetsInitialSessionData() throws Exception {
        assertEquals(0,_sessionManager.getSessionData().size());
    }

    @SmallTest
    public void testStartSessionCreatesNewSessionBuilder() throws Exception {
        _sessionManager.startSession(1000, _mockGoogleApiClient);

        verify(_mockPlayServices, timeout(2000).times(1)).newSessionBuilder();
    }

    @SmallTest
    public void testStartSessionSetSessionBuilderSessionIdentifier() throws Exception {
        when(_mockPlayServices.generateSessionIdentifier(anyLong())).thenReturn("MockSessionIdentifier");

        _sessionManager.startSession(1000, _mockGoogleApiClient);

        verify(_mockSessionBuilder,timeout(2000).times(1)).setIdentifier("MockSessionIdentifier");
    }

    @SmallTest
    public void testStartSessionSetSessionBuilderSessionName() throws Exception {
        when(_mockPlayServices.generateSessionName()).thenReturn("MockSessionName");

        _sessionManager.startSession(1000, _mockGoogleApiClient);

        verify(_mockSessionBuilder,timeout(2000).times(1)).setName("MockSessionName");
    }

    @SmallTest
    public void testStartSessionSetSessionBuilderStartTime() throws Exception {
        _sessionManager.startSession(1000, _mockGoogleApiClient);

        verify(_mockSessionBuilder,timeout(2000).times(1)).setStartTime(anyLong(),any(TimeUnit.class));
    }

    @SmallTest
    public void testStartSessionSetsSession() throws Exception {
        _sessionManager.startSession(1000, _mockGoogleApiClient);

        verify(_mockSessionsApi,timeout(2000).times(1)).startSession(any(GoogleApiClient.class),any(Session.class));
    }

    @SmallTest
    public void testAddDataPointAddsSessionData() throws Exception {
        _sessionManager.startSession(1000, _mockGoogleApiClient);
        _sessionManager.addDataPoint(1000, DetectedActivity.ON_BICYCLE);
        assertEquals(1, _sessionManager.getSessionData().size());
    }

    @SmallTest
    public void testAddDataPointWhenActivitySameAsPreviousDoesNotAddSessionData() throws Exception {
        _sessionManager.startSession(1000, _mockGoogleApiClient);
        _sessionManager.addDataPoint(1000, DetectedActivity.ON_BICYCLE);
        _sessionManager.addDataPoint(1000, DetectedActivity.ON_BICYCLE);
        assertEquals(1, _sessionManager.getSessionData().size());
    }

    @SmallTest
    public void testAddDataPointWhenActivityNotSameAsPreviousDoesAddSessionData() throws Exception {
        _sessionManager.startSession(1000, _mockGoogleApiClient);
        _sessionManager.addDataPoint(1000, DetectedActivity.ON_BICYCLE);
        _sessionManager.addDataPoint(1000, DetectedActivity.ON_FOOT);
        assertEquals(2, _sessionManager.getSessionData().size());
    }

    @SmallTest
    public void testSaveActiveSessionClearsSessionData() throws Exception {
        startSession(1000);
        _sessionManager.addDataPoint(2000, DetectedActivity.ON_BICYCLE);
        _sessionManager.saveActiveSession(3000);
        DataSet dataSet = getDataSet();
        assertEquals(0,_sessionManager.getSessionData().size());
    }

    @SmallTest
    public void testSaveActiveSessionCallsStopSession() {
        when(_mockPlayServices.generateSessionIdentifier(anyLong())).thenReturn("MockSessionIdentifier");
        _sessionManager.startSession(1000, _mockGoogleApiClient);
        _sessionManager.saveActiveSession(2000);
        verify(_mockSessionsApi, times(1)).stopSession(_mockGoogleApiClient, "MockSessionIdentifier");
    }

    @SmallTest
    public void testSaveActiveSessionSetsCallBack() {
        when(_mockPlayServices.generateSessionIdentifier(anyLong())).thenReturn("MockSessionIdentifier");

        _sessionManager.startSession(1000, _mockGoogleApiClient);
        _sessionManager.saveActiveSession(2000);
        verify(_mockResult, times(1)).setResultCallback(any(ResultCallback.class));
    }

    @SmallTest
    public void testSaveActiveSessionCallBackWithNoRecognisedActivityCreates1DataPoints() {

        // setup the mock to return the same start time as set
        startSession(1000);

        when(_mockSession.getStartTime(TimeUnit.MILLISECONDS)).thenReturn((long)1000);

        _sessionManager.saveActiveSession(2000);
        DataSet dataSet = getDataSet();

        assertEquals(1,dataSet.getDataPoints().size());
        assertEquals("Start time should equal session start time",1000,dataSet.getDataPoints().get(0).getStartTime(TimeUnit.MILLISECONDS));
        assertEquals("End time should equal session end time", 2000,dataSet.getDataPoints().get(0).getEndTime(TimeUnit.MILLISECONDS));
        assertEquals("Activity should equal unknown", FitnessActivities.UNKNOWN, dataSet.getDataPoints().get(0).getValue(Field.FIELD_ACTIVITY).asActivity());
    }

    @SmallTest
    public void testNewWALKINGActivityEventCreates1DataPoints() {
        startSession(1000);

        _sessionManager.addDataPoint(2000, DetectedActivity.WALKING);

        _sessionManager.saveActiveSession(3000);

        DataSet dataSet = getDataSet();

        assertEquals(1,dataSet.getDataPoints().size());
        assertEquals("Start time should equal data start time",2000,dataSet.getDataPoints().get(0).getStartTime(TimeUnit.MILLISECONDS));
        assertEquals("End time should equal session end time", 3000,dataSet.getDataPoints().get(0).getEndTime(TimeUnit.MILLISECONDS));
        assertEquals("Activity should equal unknown", FitnessActivities.WALKING, dataSet.getDataPoints().get(0).getValue(Field.FIELD_ACTIVITY).asActivity());
    }

    @SmallTest
    public void testNewON_BICYCLEActivityEventCreates1DataPoints() {
        startSession(1000);

        _sessionManager.addDataPoint(2000, DetectedActivity.ON_BICYCLE);

        _sessionManager.saveActiveSession(3000);

        DataSet dataSet = getDataSet();

        assertEquals(1,dataSet.getDataPoints().size());
        assertEquals("Start time should equal data start time",2000,dataSet.getDataPoints().get(0).getStartTime(TimeUnit.MILLISECONDS));
        assertEquals("End time should equal session end time", 3000,dataSet.getDataPoints().get(0).getEndTime(TimeUnit.MILLISECONDS));
        assertEquals("Activity should equal unknown", FitnessActivities.BIKING, dataSet.getDataPoints().get(0).getValue(Field.FIELD_ACTIVITY).asActivity());
    }

    @SmallTest
    public void testNewRUNNINGActivityEventCreates1DataPoints() {
        startSession(1000);

        _sessionManager.addDataPoint(2000, DetectedActivity.RUNNING);

        _sessionManager.saveActiveSession(3000);

        DataSet dataSet = getDataSet();

        assertEquals(1,dataSet.getDataPoints().size());
        assertEquals("Start time should equal data start time",2000,dataSet.getDataPoints().get(0).getStartTime(TimeUnit.MILLISECONDS));
        assertEquals("End time should equal session end time", 3000,dataSet.getDataPoints().get(0).getEndTime(TimeUnit.MILLISECONDS));
        assertEquals("Activity should equal unknown", FitnessActivities.RUNNING, dataSet.getDataPoints().get(0).getValue(Field.FIELD_ACTIVITY).asActivity());
    }


    @SmallTest
    public void test2DuplicateActivityEventsCreates1DataPoints() {
        startSession(1000);

        _sessionManager.addDataPoint(2000, DetectedActivity.RUNNING);
        _sessionManager.addDataPoint(3000, DetectedActivity.RUNNING);

        _sessionManager.saveActiveSession(4000);

        DataSet dataSet = getDataSet();

        assertEquals(1,dataSet.getDataPoints().size());
        assertEquals("Start time should equal data start time",2000,dataSet.getDataPoints().get(0).getStartTime(TimeUnit.MILLISECONDS));
        assertEquals("End time should equal session end time", 4000,dataSet.getDataPoints().get(0).getEndTime(TimeUnit.MILLISECONDS));
        assertEquals("Activity should equal unknown", FitnessActivities.RUNNING, dataSet.getDataPoints().get(0).getValue(Field.FIELD_ACTIVITY).asActivity());
    }

    @SmallTest
    public void test2UniqueActivityEventsCreates2DataPoints() {
        startSession(1000);

        _sessionManager.addDataPoint(2000, DetectedActivity.RUNNING);
        _sessionManager.addDataPoint(3000, DetectedActivity.WALKING);

        _sessionManager.saveActiveSession(4000);

        DataSet dataSet = getDataSet();

        assertEquals(2,dataSet.getDataPoints().size());

        assertEquals("Start time should equal data start time",2000,dataSet.getDataPoints().get(0).getStartTime(TimeUnit.MILLISECONDS));
        assertEquals("End time should equal session end time", 3000,dataSet.getDataPoints().get(0).getEndTime(TimeUnit.MILLISECONDS));
        assertEquals("Activity should equal unknown", FitnessActivities.RUNNING, dataSet.getDataPoints().get(0).getValue(Field.FIELD_ACTIVITY).asActivity());

        assertEquals("Start time should equal data start time",3000,dataSet.getDataPoints().get(1).getStartTime(TimeUnit.MILLISECONDS));
        assertEquals("End time should equal session end time", 4000,dataSet.getDataPoints().get(1).getEndTime(TimeUnit.MILLISECONDS));
        assertEquals("Activity should equal unknown", FitnessActivities.WALKING, dataSet.getDataPoints().get(1).getValue(Field.FIELD_ACTIVITY).asActivity());
    }

    private void startSession(long startTime) {
        when(_mockPlayServices.generateSessionIdentifier(anyLong())).thenReturn("MockSessionIdentifier");
        when(_mockSession.getStartTime(TimeUnit.MILLISECONDS)).thenReturn(startTime);
        _sessionManager.startSession(startTime, _mockGoogleApiClient);
    }

    private DataSet getDataSet() {
        ArrayList<Session> sessions = new ArrayList<>();
        sessions.add(_mockSession);

        ArgumentCaptor<ResultCallback> captor = ArgumentCaptor.forClass(ResultCallback.class);
        verify(_mockResult, times(1)).setResultCallback(captor.capture());

        ArgumentCaptor<Session> sessionArgumentCaptor = ArgumentCaptor.forClass(Session.class);

        captor.getValue().onResult(new SessionStopResult(new Status(1), sessions));

        ArgumentCaptor<DataSet> dataSetArgumentCaptor = ArgumentCaptor.forClass(DataSet.class);
        verify(_mockPlayServices,times(1)).newSessionInsertRequest(sessionArgumentCaptor.capture(), dataSetArgumentCaptor.capture());
        assertEquals(_mockSession, sessionArgumentCaptor.getValue());

        return dataSetArgumentCaptor.getValue();
    }
}
