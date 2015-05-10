package com.njackson.test.utils.services;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.events.ActivityRecognitionCommand.ActivityRecognitionChangeState;
import com.njackson.events.GPSServiceCommand.GPSChangeState;
import com.njackson.events.GPSServiceCommand.GPSStatus;
import com.njackson.events.GoogleFitCommand.GoogleFitChangeState;
import com.njackson.events.LiveServiceCommand.LiveChangeState;
import com.njackson.events.base.BaseChangeState;
import com.njackson.oruxmaps.OruxMaps;
import com.njackson.service.MainService;
import com.njackson.utils.services.ServiceStarter;
import com.squareup.otto.Bus;

import org.mockito.ArgumentCaptor;

import java.util.ArrayList;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by njackson on 03/01/15.
 */
public class ServiceStarterTest extends AndroidTestCase {

    Context _mockContext;
    private ServiceStarter _serviceStarter;
    private SharedPreferences _mockPreferences;
    private Bus _mockBus;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());
        setupMocks();

        _serviceStarter = new ServiceStarter(_mockContext, _mockPreferences, _mockBus);
    }

    private void setupMocks() {
        _mockContext = mock(Context.class);
        _mockPreferences = mock(SharedPreferences.class);
        _mockBus = mock(Bus.class);

        when(_mockPreferences.getString("REFRESH_INTERVAL", "1000")).thenReturn("1500");
    }

    @SmallTest
    public void testStartActivityServiceStartsMainServiceIfNotRunning() throws Exception {
        setupMockServiceStopped();
        _serviceStarter.startActivityService();

        verify(_mockContext, times(1)).startService(any(Intent.class));
    }

    @SmallTest
    public void testStartActivityServiceDoesNotStartsMainServiceIfRunning() throws Exception {
        setupMockServiceRunning();
        _serviceStarter.startActivityService();

        verify(_mockContext, times(0)).startService(any(Intent.class));
    }

    @SmallTest
    public void testStartActivityServiceSendsMessage() throws Exception {
        setupMockServiceRunning();
        _serviceStarter.startActivityService();

        ArgumentCaptor<BaseChangeState> statusCaptor = ArgumentCaptor.forClass(BaseChangeState.class);

        verify(_mockBus,times(1)).post(statusCaptor.capture());
        assertEquals(BaseChangeState.State.START, getStateFromCaptor(statusCaptor, ActivityRecognitionChangeState.class).getState());
    }

    @SmallTest
    public void testStopActivityServiceSendsMessage() throws Exception {
        setupMockServiceRunning();
        _serviceStarter.stopActivityService();

        ArgumentCaptor<BaseChangeState> statusCaptor = ArgumentCaptor.forClass(BaseChangeState.class);

        verify(_mockBus,times(1)).post(statusCaptor.capture());
        assertEquals(BaseChangeState.State.STOP, getStateFromCaptor(statusCaptor, ActivityRecognitionChangeState.class).getState());
    }

    @SmallTest
    public void testStartsLocationServicesStartsMainServiceIfNotRunning() throws Exception {
        setupMockServiceStopped();
        _serviceStarter.startLocationServices();

        verify(_mockContext, atLeast(1)).startService(any(Intent.class));
    }

    @SmallTest
    public void testStartsLocationServicesDoesNotStartsMainServiceIfRunning() throws Exception {
        setupMockServiceRunning();
        _serviceStarter.startLocationServices();

        verify(_mockContext, times(0)).startService(any(Intent.class));
    }

    @SmallTest
    public void testStartsGPSService() throws Exception {
        setupMockServiceRunning();
        _serviceStarter.startLocationServices();

        ArgumentCaptor<BaseChangeState> statusCaptor = ArgumentCaptor.forClass(BaseChangeState.class);

        verify(_mockBus,atLeast(1)).post(statusCaptor.capture());
        assertEquals(BaseChangeState.State.START, getStateFromCaptor(statusCaptor, GPSChangeState.class).getState());
    }

    @SmallTest
    public void testStartsGPSServiceWithRefreshInterval() throws Exception {
        setupMockServiceRunning();
        _serviceStarter.startLocationServices();

        ArgumentCaptor<BaseChangeState> statusCaptor = ArgumentCaptor.forClass(BaseChangeState.class);

        verify(_mockBus,atLeast(1)).post(statusCaptor.capture());
        assertEquals(1500, ((GPSChangeState)getStateFromCaptor(statusCaptor,GPSChangeState.class)).getRefreshInterval());
    }

    @SmallTest
    public void testStopsGPSService() throws Exception {
        setupMockServiceRunning();
        _serviceStarter.stopLocationServices();

        ArgumentCaptor<BaseChangeState> statusCaptor = ArgumentCaptor.forClass(BaseChangeState.class);

        verify(_mockBus,atLeast(1)).post(statusCaptor.capture());
        assertEquals(BaseChangeState.State.STOP, getStateFromCaptor(statusCaptor,GPSChangeState.class).getState());
    }

    @SmallTest
    public void testStartsLiveTrackingService() throws Exception {
        setupMockServiceRunning();
        _serviceStarter.startLocationServices();
        ArgumentCaptor<BaseChangeState> statusCaptor = ArgumentCaptor.forClass(BaseChangeState.class);

        verify(_mockBus,atLeast(1)).post(statusCaptor.capture());
        assertEquals(BaseChangeState.State.START, getStateFromCaptor(statusCaptor, LiveChangeState.class).getState());
    }

    @SmallTest
    public void testStopsLiveTrackingService() throws Exception {
        setupMockServiceRunning();
        _serviceStarter.stopLocationServices();
        ArgumentCaptor<BaseChangeState> statusCaptor = ArgumentCaptor.forClass(BaseChangeState.class);

        verify(_mockBus,atLeast(1)).post(statusCaptor.capture());
        assertEquals(BaseChangeState.State.STOP, getStateFromCaptor(statusCaptor, LiveChangeState.class).getState());
    }
/* Disable Google Fit for v2 (shifted to v2.1+)
    @SmallTest
    public void testStartsGoogleFitServiceWhenEnabled() throws Exception {
        when(_mockPreferences.getBoolean("GOOGLE_FIT",false)).thenReturn(true);

        setupMockServiceRunning();
        _serviceStarter.startLocationServices();
        ArgumentCaptor<BaseChangeState> statusCaptor = ArgumentCaptor.forClass(BaseChangeState.class);

        verify(_mockBus,atLeast(1)).post(statusCaptor.capture());
        assertEquals(BaseChangeState.State.START, getStateFromCaptor(statusCaptor, GoogleFitChangeState.class).getState());
    }
*/
/* Disable Google Fit for v2 (shifted to v2.1+)
    @SmallTest
    public void testDoesNOTStartsGoogleFitServiceWhenEnabled() throws Exception {
        when(_mockPreferences.getBoolean("GOOGLE_FIT",false)).thenReturn(false);

        setupMockServiceRunning();
        _serviceStarter.startLocationServices();
        ArgumentCaptor<BaseChangeState> statusCaptor = ArgumentCaptor.forClass(BaseChangeState.class);

        verify(_mockBus,atLeast(1)).post(statusCaptor.capture());
        assertNull(getStateFromCaptor(statusCaptor, GoogleFitChangeState.class));
    }
*/
    @SmallTest
    public void testStopsGoogleFitService() throws Exception {
        setupMockServiceRunning();
        _serviceStarter.stopLocationServices();
        ArgumentCaptor<BaseChangeState> statusCaptor = ArgumentCaptor.forClass(BaseChangeState.class);

        verify(_mockBus,atLeast(1)).post(statusCaptor.capture());
        assertEquals(BaseChangeState.State.STOP, getStateFromCaptor(statusCaptor, GoogleFitChangeState.class).getState());
    }

    @SmallTest
    public void testStartsActivityServiceWhenEnabled() throws Exception {
        when(_mockPreferences.getBoolean("ACTIVITY_RECOGNITION",false)).thenReturn(true);

        setupMockServiceRunning();
        _serviceStarter.startLocationServices();
        ArgumentCaptor<BaseChangeState> statusCaptor = ArgumentCaptor.forClass(BaseChangeState.class);

        verify(_mockBus,atLeast(1)).post(statusCaptor.capture());
        assertEquals(BaseChangeState.State.START, getStateFromCaptor(statusCaptor, ActivityRecognitionChangeState.class).getState());
    }
/* Disable Google Fit for v2 (shifted to v2.1+)
    @SmallTest
    public void testStartsActivityServiceWhenGoogleFitEnabled() throws Exception {
        when(_mockPreferences.getBoolean("GOOGLE_FIT",false)).thenReturn(true);

        setupMockServiceRunning();
        _serviceStarter.startLocationServices();
        ArgumentCaptor<BaseChangeState> statusCaptor = ArgumentCaptor.forClass(BaseChangeState.class);

        verify(_mockBus,atLeast(1)).post(statusCaptor.capture());
        assertEquals(BaseChangeState.State.START, getStateFromCaptor(statusCaptor, ActivityRecognitionChangeState.class).getState());
    }
*/
    @SmallTest
    public void testDoesNOTActivityServiceWhenEnabled() throws Exception {
        when(_mockPreferences.getBoolean("ACTIVITY_RECOGNITION",false)).thenReturn(false);

        setupMockServiceRunning();
        _serviceStarter.startLocationServices();
        ArgumentCaptor<BaseChangeState> statusCaptor = ArgumentCaptor.forClass(BaseChangeState.class);

        verify(_mockBus,atLeast(1)).post(statusCaptor.capture());
        assertNull(getStateFromCaptor(statusCaptor, ActivityRecognitionChangeState.class));
    }

    @SmallTest
    public void testStopsActivityService() throws Exception {
        setupMockServiceRunning();
        _serviceStarter.stopLocationServices();
        ArgumentCaptor<BaseChangeState> statusCaptor = ArgumentCaptor.forClass(BaseChangeState.class);

        verify(_mockBus,atLeast(1)).post(statusCaptor.capture());
        assertEquals(BaseChangeState.State.STOP, getStateFromCaptor(statusCaptor, ActivityRecognitionChangeState.class).getState());
    }

    @SmallTest
    public void testDoesNotStopActivityServiceWhenAutoStart() throws Exception {
        when(_mockPreferences.getBoolean("ACTIVITY_RECOGNITION",false)).thenReturn(true);
        setupMockServiceRunning();
        _serviceStarter.stopLocationServices();
        ArgumentCaptor<BaseChangeState> statusCaptor = ArgumentCaptor.forClass(BaseChangeState.class);

        verify(_mockBus,atLeast(1)).post(statusCaptor.capture());
        assertNull(getStateFromCaptor(statusCaptor, ActivityRecognitionChangeState.class));
    }

    @SmallTest
    public void testBroadcastLocationStateSendsMessage() {
        setupMockServiceRunning();
        _serviceStarter.broadcastLocationState();

        ArgumentCaptor<BaseChangeState> statusCaptor = ArgumentCaptor.forClass(BaseChangeState.class);

        verify(_mockBus,times(1)).post(statusCaptor.capture());
        assertEquals(BaseChangeState.State.ANNOUNCE_STATE, getStateFromCaptor(statusCaptor, GPSChangeState.class).getState());
    }

    @SmallTest
    public void testServiceRunningReturnsTrueWhenServiceStarted() throws Exception {
        setupMockServiceRunning();

        assertEquals(true, _serviceStarter.serviceRunning(MainService.class));
    }

    @SmallTest
    public void testServiceRunningReturnsFalseWhenServiceNotRunning() throws Exception {
        setupMockServiceStopped();

        assertEquals(false,_serviceStarter.serviceRunning(OruxMaps.class));
    }

    private void setupMockServiceRunning() {
        ActivityManager manager = mock(ActivityManager.class);
        ArrayList<ActivityManager.RunningServiceInfo> runningServices = new ArrayList<ActivityManager.RunningServiceInfo>();
        ActivityManager.RunningServiceInfo info = new ActivityManager.RunningServiceInfo();
        info.service = new ComponentName(MainService.class.getPackage().getName(),MainService.class.getName());
        runningServices.add(info);

        when(manager.getRunningServices(Integer.MAX_VALUE)).thenReturn(runningServices);
        when(_mockContext.getSystemService(getContext().ACTIVITY_SERVICE)).thenReturn(manager);
    }

    private void setupMockServiceStopped() {
        ActivityManager manager = mock(ActivityManager.class);
        ArrayList<ActivityManager.RunningServiceInfo> runningServices = new ArrayList<ActivityManager.RunningServiceInfo>();
        ActivityManager.RunningServiceInfo info = new ActivityManager.RunningServiceInfo();
        info.service = new ComponentName(MainService.class.getPackage().getName(),GPSStatus.class.getName());
        runningServices.add(info);

        when(manager.getRunningServices(Integer.MAX_VALUE)).thenReturn(runningServices);
        when(_mockContext.getSystemService(getContext().ACTIVITY_SERVICE)).thenReturn(manager);
    }

    private BaseChangeState getStateFromCaptor(ArgumentCaptor<BaseChangeState> states, Class<?> serviceClass) {
        for(BaseChangeState state : states.getAllValues()) {
            if(state.getClass().getName().compareTo(serviceClass.getName()) == 0) {
                return state;
            }
        }

        return null;
    }
}
