package com.njackson.utils.googleplay;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.fitness.SessionsApi;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.result.SessionStopResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

/**
 * Created by njackson on 11/01/15.
 */
public class GoogleFitSessionManager implements IGoogleFitSessionManager {

    private final SessionsApi _sessionsApi;
    private final Context _context;
    private IGooglePlayServices _playServices;
    private Session _session;
    private String _sessionIdentifier;
    private GoogleApiClient _googleAPIClient;
    private DataSet _activitySegments;

    private List<SessionData> _data;

    public List<SessionData> getSessionData() { return _data; }

    public GoogleFitSessionManager(Context context ,IGooglePlayServices playServices, SessionsApi sessionsApi) {
        _playServices = playServices;
        _sessionsApi = sessionsApi;
        _context = context;
    }

    @Override
    public void startSession(long startTime, GoogleApiClient client) {
        _googleAPIClient = client;
        _session = createSession(startTime);
        _activitySegments = createDataSource();
        _data = new ArrayList<SessionData>();
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
        if(_data.size() == 0 || _data.get(_data.size() - 1).getActivity() != activity) {
            SessionData sessionData = new SessionData(startTime, activity);
            _data.add(sessionData);
        }
    }

    @Override
    public void saveActiveSession() {
        PendingResult<SessionStopResult> pendingResult = _sessionsApi.stopSession(_googleAPIClient, _sessionIdentifier);
        pendingResult.setResultCallback(new ResultCallback<SessionStopResult>() {
            @Override
            public void onResult(SessionStopResult sessionStopResult) {
                if (sessionStopResult.getSessions().size() > 0) {
                    for (Session session : sessionStopResult.getSessions()) {

                    }
                }
            }
        });
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
