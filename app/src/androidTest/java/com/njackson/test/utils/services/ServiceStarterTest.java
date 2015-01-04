package com.njackson.test.utils.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationListener;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.activityrecognition.ActivityRecognitionService;
import com.njackson.events.UI.StartButtonTouchedEvent;
import com.njackson.events.UI.StopButtonTouchedEvent;
import com.njackson.gps.GPSService;
import com.njackson.live.LiveService;
import com.njackson.test.testUtils.Services;
import com.njackson.utils.services.ServiceStarter;
import com.njackson.virtualpebble.PebbleService;

import org.mockito.ArgumentCaptor;

import java.util.List;

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

    @Override
    public void setUp() throws Exception {
        super.setUp();

        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());
        setupMocks();

        _serviceStarter = new ServiceStarter(_mockContext, _mockPreferences);
    }

    private void setupMocks() {
        _mockContext = mock(Context.class);
        _mockPreferences = mock(SharedPreferences.class);

        when(_mockPreferences.getString("REFRESH_INTERVAL","1000")).thenReturn("1500");
    }

    @SmallTest
    public void testStartsGPSService() throws Exception {
        _serviceStarter.startLocationServices();
        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);

        verify(_mockContext,times(3)).startService(intentArgumentCaptor.capture());

        List<Intent> intents = intentArgumentCaptor.getAllValues();
        assertTrue(checkComponentInCaptor(intents, GPSService.class));
    }

    @SmallTest
    public void testStartsGPSServiceWithRefreshInterval() throws Exception {
        _serviceStarter.startLocationServices();
        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);

        verify(_mockContext,times(3)).startService(intentArgumentCaptor.capture());

        List<Intent> intents = intentArgumentCaptor.getAllValues();
        Intent startIntent = getIntentFromCaptor(intents,GPSService.class);

        int refreshInterval = startIntent.getIntExtra("REFRESH_INTERVAL",1000);
        assertEquals(1500,refreshInterval);
    }

    @SmallTest
    public void testStopsGPSService() throws Exception {
        _serviceStarter.stopLocationServices();
        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);

        verify(_mockContext,times(3)).stopService(intentArgumentCaptor.capture());

        List<Intent> intents = intentArgumentCaptor.getAllValues();
        assertTrue(checkComponentInCaptor(intents,GPSService.class));
    }

    @SmallTest
    public void testStartsPebbleBikeService() throws Exception {
        _serviceStarter.startLocationServices();
        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);

        verify(_mockContext,times(3)).startService(intentArgumentCaptor.capture());

        List<Intent> intents = intentArgumentCaptor.getAllValues();
        assertTrue(checkComponentInCaptor(intents,PebbleService.class));
    }

    @SmallTest
    public void testStopsPebbleBikeService() throws Exception {
        _serviceStarter.stopLocationServices();
        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);

        verify(_mockContext,times(3)).stopService(intentArgumentCaptor.capture());

        List<Intent> intents = intentArgumentCaptor.getAllValues();
        assertTrue(checkComponentInCaptor(intents,PebbleService.class));
    }

    @SmallTest
    public void testStartsLiveTrackingService() throws Exception {
        _serviceStarter.startLocationServices();
        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);

        verify(_mockContext,times(3)).startService(intentArgumentCaptor.capture());

        List<Intent> intents = intentArgumentCaptor.getAllValues();
        assertTrue(checkComponentInCaptor(intents,LiveService.class));
    }

    @SmallTest
    public void testStopsLiveTrackingService() throws Exception {
        _serviceStarter.stopLocationServices();
        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);

        verify(_mockContext,times(3)).stopService(intentArgumentCaptor.capture());

        List<Intent> intents = intentArgumentCaptor.getAllValues();
        assertTrue(checkComponentInCaptor(intents,LiveService.class));
    }

    @SmallTest
    public void testStartsRecognitionService() throws Exception {
        _serviceStarter.startRecognitionServices();
        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);

        verify(_mockContext,times(1)).startService(intentArgumentCaptor.capture());

        List<Intent> intents = intentArgumentCaptor.getAllValues();
        assertTrue(checkComponentInCaptor(intents,ActivityRecognitionService.class));
    }

    @SmallTest
    public void testStopsRecognitionService() throws Exception {
        _serviceStarter.stopRecognitionServices();
        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);

        verify(_mockContext,times(1)).stopService(intentArgumentCaptor.capture());

        List<Intent> intents = intentArgumentCaptor.getAllValues();
        assertTrue(checkComponentInCaptor(intents,ActivityRecognitionService.class));
    }

    private Intent getIntentFromCaptor(List<Intent> intents, Class<?> serviceClass) {
        for(Intent intent : intents) {
            if(intent.getComponent().getClassName().compareTo(serviceClass.getName()) == 0) {
                return intent;
            }
        }

        return null;
    }

    private boolean checkComponentInCaptor(List<Intent> intents, Class<?> serviceClass) {
        return (getIntentFromCaptor(intents,serviceClass) != null);
    }

}
