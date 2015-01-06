package com.njackson.test.gps;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.application.modules.PebbleBikeModule;
import com.njackson.events.GPSService.ChangeRefreshInterval;
import com.njackson.events.GPSService.ResetGPSState;
import com.njackson.events.GPSService.CurrentState;
import com.njackson.events.GPSService.NewLocation;
import com.njackson.gps.GPSSensorEventListener;
import com.njackson.gps.GPSService;
import com.njackson.gps.IGPSServiceStarterForeground;
import com.njackson.test.application.TestApplication;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.mockito.ArgumentCaptor;
import org.mockito.verification.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by server on 28/04/2014.
 */
public class GPSServiceTest extends ServiceTestCase<GPSService>{

    @Inject Bus _bus = new Bus();
    @Inject LocationManager _mockLocationManager;
    @Inject SensorManager _mockSensorManager;
    @Inject SharedPreferences _mockPreferences;
    private SharedPreferences.Editor _mockEditor;
    private static IGPSServiceStarterForeground _mockServiceStarter;

    private GPSService _service;
    private Context _applicationContext;

    private NewLocation _locationEventResults;

    private CurrentState _gpsStatusEvent;

    private CountDownLatch _stateLatch;
    private CountDownLatch _newLocationLatch;

    @Module(
            includes = PebbleBikeModule.class,
            injects = GPSServiceTest.class,
            overrides = true,
            complete = false
    )
    static class TestModule {
        @Provides
        @Singleton
        LocationManager provideLocationManager() {
            return mock(LocationManager.class);
        }

        @Provides
        @Singleton
        SensorManager provideSensorManager() { return mock(SensorManager.class); }

        @Provides
        @Singleton
        SharedPreferences provideSharedPreferences() {
            return mock(SharedPreferences.class);
        }

        @Provides
        IGPSServiceStarterForeground providesForegroundServiceStarter() { return _mockServiceStarter; }
    }


    public GPSServiceTest(Class<GPSService> serviceClass) {
        super(serviceClass);
    }

    public GPSServiceTest() {
        super(GPSService.class);
    }

    //Bus Subscriptions
    @Subscribe
    public void onNewLocationEvent(NewLocation event) {
        _locationEventResults = event;
        _newLocationLatch.countDown();
    }

    @Subscribe
    public void onGPSStatusEvent(CurrentState event) {
        _gpsStatusEvent = event;

        _stateLatch.countDown();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        System.setProperty("dexmaker.dexcache", getSystemContext().getCacheDir().getPath());

        TestApplication app = new TestApplication();
        app.setObjectGraph(ObjectGraph.create(TestModule.class));
        app.inject(this);
        _bus.register(this);

        setApplication(app);

        setupMocks();
        GPSService.isJUnit = true;

        _locationEventResults = null; // reset the event results
        _stateLatch = new CountDownLatch(1);
        _newLocationLatch = new CountDownLatch(1);
    }

    private void startService() throws Exception {
        Intent startIntent = new Intent(getSystemContext(), GPSService.class);
        startService(startIntent);
        _service = getService();
        _stateLatch.await(2000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void tearDown() throws Exception {
        reset(_mockLocationManager);
        super.tearDown();
    }


    private void setupMocks() {
        _mockServiceStarter = mock(IGPSServiceStarterForeground.class);
        _mockEditor = mock(SharedPreferences.Editor.class, RETURNS_DEEP_STUBS);
        when(_mockPreferences.edit()).thenReturn(_mockEditor);
    }

    @SmallTest
    public void testOnBindReturnsNull() throws Exception {
        startService();
        IBinder binder = _service.onBind(new Intent());

        assertNull(binder);
    }

    @SmallTest
    public void testBroadcastEventOnLocationDisabled() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(false);

        startService();

        assertEquals(CurrentState.State.DISABLED, _gpsStatusEvent.getState());
    }

    @SmallTest
    public void testBroadcastEventOnLocationChange() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);

        startService();

        ArgumentCaptor<LocationListener> locationListenerCaptor = ArgumentCaptor.forClass(LocationListener.class);
        verify(_mockLocationManager).requestLocationUpdates(
                anyString(),
                anyLong(),
                anyFloat(),
                locationListenerCaptor.capture());

        Location location = new Location("location");
        LocationListener listenerArgument = locationListenerCaptor.getValue();
        listenerArgument.onLocationChanged(location);

        _newLocationLatch.await(2000, TimeUnit.MILLISECONDS);
        assertNotNull(_locationEventResults);
    }

    @SmallTest
    public void testHandlesGPSLocationReset() throws Exception {
        startService();

        _bus.post(new ResetGPSState());

        verify(_mockEditor, timeout(200).times(1)).putFloat("GPS_DISTANCE", 0.0f);
        verify(_mockEditor,timeout(200).times(1)).commit();
    }

    @SmallTest
    public void testHandlesGPSStartCommand() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);
        startService();

        verify(_mockLocationManager,times(1)).requestLocationUpdates(
                anyString(),
                anyLong(),
                anyFloat(),
                any(LocationListener.class));
    }

    @SmallTest
    public void testHandlesRefreshInterval() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);

        startService();

        ArgumentCaptor<LocationListener> locationListenerCaptor = ArgumentCaptor.forClass(LocationListener.class);
        verify(_mockLocationManager).requestLocationUpdates(
                anyString(),
                anyLong(),
                anyFloat(),
                locationListenerCaptor.capture());

        int refreshInterval = 200;
        _bus.post(new ChangeRefreshInterval(refreshInterval));

        verify(_mockLocationManager, timeout(200).times(1)).removeUpdates((LocationListener) anyObject());
        verify(_mockLocationManager, timeout(200).times(1)).requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                refreshInterval,
                2,
                locationListenerCaptor.getValue()
        );
    }

    @SmallTest
    public void testSavesStateOnDestroy() throws Exception {
        startService();
        shutdownService();

        verify(_mockEditor, times(1)).commit();
    }

    @SmallTest
    public void testRegistersNmeaListenerOnCreate() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);
        startService();

        verify(_mockLocationManager,timeout(2000).times(1)).addNmeaListener(any(GpsStatus.NmeaListener.class));
    }

    @SmallTest
    public void testRemovesNmeaListenerOnDestroy() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);
        startService();
        shutdownService();

        verify(_mockLocationManager,timeout(2000).times(1)).removeNmeaListener(any(GpsStatus.NmeaListener.class));
    }

    @SmallTest
    public void testRegistersSensorOnCreate() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);
        startService();

        verify(_mockSensorManager,timeout(2000).times(1)).registerListener(any(GPSSensorEventListener.class),any(Sensor.class),anyInt());
    }

    @SmallTest
    public void testRemovesSensorOnDestroy() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);
        startService();
        shutdownService();

        verify(_mockSensorManager,timeout(2000).times(1)).unregisterListener(any(GPSSensorEventListener.class));
    }

    @SmallTest
    public void testStartsServiceForeground() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);
        startService();

        verify(_mockServiceStarter,timeout(2000).times(1)).startServiceForeground(any(GPSService.class),anyString(),anyString());
    }

    @SmallTest
    public void testStopsServiceForeground() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);
        startService();
        shutdownService();

        verify(_mockServiceStarter,timeout(2000).times(1)).stopServiceForeground(any(GPSService.class));
    }
}