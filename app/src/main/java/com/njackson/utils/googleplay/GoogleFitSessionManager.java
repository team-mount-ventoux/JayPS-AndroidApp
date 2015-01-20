package com.njackson.utils.googleplay;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.SessionsApi;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.result.SessionStopResult;
import com.njackson.adapters.DetectedToFitnessActivityAdapater;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by njackson on 11/01/15.
 */
public class GoogleFitSessionManager implements IGoogleFitSessionManager {

    private static final String TAG = "GoogleFit Session Manager";
    private final SessionsApi _sessionsApi;
    private final Context _context;
    private IGooglePlayServices _playServices;
    private Session _session;
    private String _sessionIdentifier;
    private GoogleApiClient _googleAPIClient;
    private DataSet _activitySegments;

    private List<SessionData> _sessionDataList;

    @Override
    public List<SessionData> getSessionData() { return _sessionDataList; }

    @Override
    public List<DataPoint> getDataPoints() { return _activitySegments.getDataPoints(); }

    public GoogleFitSessionManager(Context context ,IGooglePlayServices playServices, SessionsApi sessionsApi) {
        _playServices = playServices;
        _sessionsApi = sessionsApi;
        _context = context;
        _sessionDataList = new ArrayList<SessionData>();
    }

    @Override
    public void startSession(long startTime, GoogleApiClient client) {
        _googleAPIClient = client;
        _session = createSession(startTime);
        _activitySegments = createDataSource();
    }

    private DataSet createDataSource() {
        DataSource activitySegmentDataSource = new DataSource.Builder()
                .setAppPackageName(_context.getPackageName())
                .setDataType(DataType.TYPE_ACTIVITY_SEGMENT)
                .setName("PebbleBike-activity segments")
                .setType(DataSource.TYPE_RAW)
                .build();
        return DataSet.create(activitySegmentDataSource);
    }

    @Override
    public void addDataPoint(long startTime, int activity) {
        if(activitySameAsLast(activity)) {
            SessionData sessionData = new SessionData(startTime, activity);
            _sessionDataList.add(sessionData);
        }
    }

    @Override
    public void saveActiveSession(final long endTime) {
        PendingResult<SessionStopResult> pendingResult = _sessionsApi.stopSession(_googleAPIClient, _sessionIdentifier);
        pendingResult.setResultCallback(new ResultCallback<SessionStopResult>() {
            @Override
            public void onResult(SessionStopResult sessionStopResult) {
                if (sessionStopResult.getSessions().size() > 0) {
                    for (Session session : sessionStopResult.getSessions()) {
                        buildDataPoints(session, endTime);
                        insertDataPoints(session);
                    }
                }
                _sessionDataList.clear();
            }
        });
    }

    private boolean activitySameAsLast(int activity) {
        return _sessionDataList.size() == 0 || _sessionDataList.get(_sessionDataList.size() - 1).getActivity() != activity;
    }

    private void buildDataPoints(Session session, long endTime) {
        if(_sessionDataList.size() < 1) {
            createUnknownDataPoint(session, endTime);
        } else {
            SessionData nextData = null;
            for(int n=0; n < _sessionDataList.size(); n++) {
                long calculatedEndTime = (_sessionDataList.size() == n + 1) ? endTime : _sessionDataList.get(n+1).getStartTime();
                createActivityDataPoint(_sessionDataList.get(n), calculatedEndTime);
            }
        }
    }

    private void createActivityDataPoint(SessionData data, long endTime) {
        DataPoint dataPoint = _activitySegments.createDataPoint()
                .setTimeInterval(data.getStartTime(), endTime, TimeUnit.MILLISECONDS);
        dataPoint.getValue(Field.FIELD_ACTIVITY).setActivity(new DetectedToFitnessActivityAdapater(data.getActivity()).getActivity());
        _activitySegments.add(dataPoint);
    }

    private void createUnknownDataPoint(Session session, long endTime) {
        DataPoint firstRunningDp = _activitySegments.createDataPoint()
                .setTimeInterval(session.getStartTime(TimeUnit.MILLISECONDS), endTime, TimeUnit.MILLISECONDS);
        firstRunningDp.getValue(Field.FIELD_ACTIVITY).setActivity(FitnessActivities.UNKNOWN);
        _activitySegments.add(firstRunningDp);
    }

    private void insertDataPoints(Session session) {
        _playServices.newSessionInsertRequest(session, _activitySegments);
        Log.d(TAG,"GoogleFit Session Saved");
    }

    private Session createSession(long startTime) {
        //String description = "";
        String sessionName = _playServices.generateSessionName();
        _sessionIdentifier = _playServices.generateSessionIdentifier(startTime);

        Session session = _playServices.newSessionBuilder()
                .setName(sessionName)
                .setIdentifier(_sessionIdentifier)
                        //.setDescription(description)
                .setStartTime(startTime, TimeUnit.MILLISECONDS)
                        // optional - if your app knows what activity:
                        //.setActivity(FitnessActivities.RUNNING)
                .build();

        _sessionsApi.startSession(_googleAPIClient, session);

        return session;
    }

    public class SessionData {
        private final long _startTime;
        private final int _activity;

        public long getStartTime() { return _startTime; }
        public int getActivity() { return _activity; }

        public SessionData(long startTime, int activity) {
            _startTime = startTime;
            _activity = activity;
        }
    }
}
