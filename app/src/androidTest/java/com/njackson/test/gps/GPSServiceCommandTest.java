package com.njackson.test.gps;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.application.modules.AndroidModule;
import com.njackson.application.modules.ForApplication;
import com.njackson.events.GPSServiceCommand.ChangeRefreshInterval;
import com.njackson.events.GPSServiceCommand.GPSChangeState;
import com.njackson.events.GPSServiceCommand.GPSStatus;
import com.njackson.events.GPSServiceCommand.ResetGPSState;
import com.njackson.events.GPSServiceCommand.NewLocation;
import com.njackson.events.base.BaseChangeState;
import com.njackson.events.base.BaseStatus;
import com.njackson.gps.GPSSensorEventListener;
import com.njackson.gps.GPSServiceCommand;
import com.njackson.gps.IForegroundServiceStarter;
import com.njackson.pebble.IMessageManager;
import com.njackson.state.GPSDataStore;
import com.njackson.state.IGPSDataStore;
import com.njackson.test.application.TestApplication;
import com.njackson.utils.time.ITime;
import com.squareup.otto.Bus;

import org.mockito.ArgumentCaptor;

import java.util.List;

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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by server on 28/04/2014.
 */
public class GPSServiceCommandTest extends AndroidTestCase {

    private static final String TAG = "PB-GPSServiceTest";

    @Inject Bus _bus = new Bus();
    @Inject SharedPreferences _mockPreferences;
    @Inject LocationManager _mockLocationManager;
    @Inject SensorManager _mockSensorManager;
    @Inject IGPSDataStore _mockDataStore;

    private static IForegroundServiceStarter _mockServiceStarter;
    private static ITime _mockTime;
    private static TestApplication _app;
    private GPSServiceCommand _serviceCommand;
    private TestApplication _mockApp;

    @Module(
            includes = AndroidModule.class,
            injects = GPSServiceCommandTest.class,
            overrides = true,
            complete = false
    )
    class TestModule {
        @Provides
        @Singleton
        SharedPreferences provideSharedPreferences() {
            return mock(SharedPreferences.class);
        }

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
        IGPSDataStore provideGPSDataStore() {
            return mock(GPSDataStore.class);
        }

        @Provides
        IForegroundServiceStarter providesForegroundServiceStarter() { return _mockServiceStarter; }

        @Provides
        ITime providesTime() { return _mockTime; }

        @Provides @Singleton @ForApplication
        Context provideApplicationContext() {
            return getContext();
        }

        @Provides @Singleton
        Bus providesBus() { return mock(Bus.class); }

        @Provides @Singleton
        public IMessageManager providesMessageManager() {
            return mock(IMessageManager.class);
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());
        _app = new TestApplication();
        _app.setObjectGraph(ObjectGraph.create(new TestModule()));
        _app.inject(this);

        setupMocks();

        _serviceCommand = new GPSServiceCommand();
    }

    private void setupMocks() {
        _mockTime = mock(ITime.class);
        _mockApp = mock(TestApplication.class);
        _mockServiceStarter = mock(IForegroundServiceStarter.class);
        when(_mockPreferences.getBoolean("PREF_DEBUG", false)).thenReturn(false);
        when(_mockPreferences.getBoolean("ENABLE_TRACKS", false)).thenReturn(false);
        when(_mockPreferences.getString("STRAVA_AUTO", "disable")).thenReturn("disable");
        when(_mockPreferences.getString("RUNKEEPER_AUTO", "disable")).thenReturn("disable");
    }

    @SmallTest
    public void testRegistersWithBusOnCreate() throws Exception {
        _serviceCommand.execute(_app);

        verify(_bus,times(1)).register(_serviceCommand);
    }

    @SmallTest
    public void testBroadcastEventOnLocationDisabled() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(false);

        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(BaseChangeState.State.START));

        ArgumentCaptor<GPSStatus> captor = ArgumentCaptor.forClass(GPSStatus.class);
        verify(_bus,timeout(1000).times(1)).post(captor.capture());

        assertEquals(BaseStatus.Status.DISABLED, captor.getValue().getStatus());
    }

    private void checkStatus(List<GPSStatus> list, int nbRequired, BaseStatus.Status firstStatus) {
        int nb = 0;
        for (int i = 0; i < list.size(); i++) {
            try {
                GPSStatus status = list.get(i);
                if (nb == 0) {
                    assertEquals(firstStatus, status.getStatus());
                }
                nb++;
            } catch (ClassCastException e) {
                // other type
            }
        }
        assertEquals(nbRequired, nb);
    }

    @SmallTest
    public void testStartEvent() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);

        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(BaseChangeState.State.START));

        ArgumentCaptor<GPSStatus> captor = ArgumentCaptor.forClass(GPSStatus.class);
        verify(_bus,timeout(1000).atLeast(1)).post(captor.capture());
        checkStatus(captor.getAllValues(), 1, BaseStatus.Status.STARTED);
    }

    @SmallTest
    public void testStopEvent() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);

        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(BaseChangeState.State.STOP));

        ArgumentCaptor<GPSStatus> captor = ArgumentCaptor.forClass(GPSStatus.class);
        verify(_bus,timeout(1000).times(1)).post(captor.capture());

        assertEquals(BaseStatus.Status.STOPPED, captor.getValue().getStatus());
    }

    @SmallTest
    public void testBroadcastStatusOnAnnounceEvent() throws Exception {
        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(BaseChangeState.State.ANNOUNCE_STATE));

        ArgumentCaptor<GPSStatus> captor = ArgumentCaptor.forClass(GPSStatus.class);
        verify(_bus,timeout(1000).times(1)).post(captor.capture());

        assertEquals(BaseStatus.Status.INITIALIZED, captor.getValue().getStatus());
    }

    @SmallTest
    public void testBroadcastEventOnLocationChange() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);
        when(_mockDataStore.getFirstLocationLattitude()).thenReturn(1.0f);
        when(_mockDataStore.getFirstLocationLongitude()).thenReturn(3.0f);
        long ts = 1200000000000l;
        when(_mockTime.getCurrentTimeMilliseconds()).thenReturn(ts);

        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(BaseChangeState.State.START));

        ArgumentCaptor<LocationListener> locationListenerCaptor = ArgumentCaptor.forClass(LocationListener.class);
        verify(_mockLocationManager,timeout(1000).times(1)).requestLocationUpdates(
                anyString(),
                anyLong(),
                anyFloat(),
                locationListenerCaptor.capture());

        Location location = new Location("location");
        LocationListener listenerArgument = locationListenerCaptor.getValue();
        when(_mockTime.getCurrentTimeMilliseconds()).thenReturn(ts+10000);
        listenerArgument.onLocationChanged(location);

        ArgumentCaptor<NewLocation> captor = ArgumentCaptor.forClass(NewLocation.class);
        verify(_bus,timeout(1000).atLeast(1)).post(captor.capture());

        int nb = 0;
        for (int i = 0; i < captor.getAllValues().size(); i++) {
            try {
                NewLocation newLocation = captor.getAllValues().get(i);
                nb++;
            } catch (ClassCastException e) {
                // other type
            }
        }
        assertEquals(2, nb);
        // 2: one with onGPSChangeState (saved one), one with onLocationChanged
    }

    @SmallTest
    public void testHandlesGPSLocationReset() throws Exception {
        _serviceCommand.execute(_app);

        _serviceCommand.onResetGPSStateEvent(new ResetGPSState());

        verify(_mockDataStore, timeout(200).times(1)).resetAllValues();
        verify(_mockDataStore,timeout(200).times(1)).commit();
    }

    @SmallTest
    public void testHandlesGPSStartCommand() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);
        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(GPSChangeState.State.START));

        verify(_mockLocationManager,timeout(2000).times(1)).requestLocationUpdates(
                anyString(),
                anyLong(),
                anyFloat(),
                any(LocationListener.class));
    }

    @SmallTest
    public void testHandlesRefreshInterval() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);

        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(GPSChangeState.State.START));

        ArgumentCaptor<LocationListener> locationListenerCaptor = ArgumentCaptor.forClass(LocationListener.class);
        verify(_mockLocationManager,timeout(200).times(1)).requestLocationUpdates(
                anyString(),
                anyLong(),
                anyFloat(),
                locationListenerCaptor.capture());


        int refreshInterval = 200;
        _serviceCommand.onGPSRefreshChangeEvent(new ChangeRefreshInterval(refreshInterval));

        verify(_mockLocationManager, timeout(200).times(1)).removeUpdates((LocationListener) anyObject());
        verify(_mockLocationManager, timeout(200).times(1)).requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                refreshInterval,
                2,
                locationListenerCaptor.getValue()
        );
    }

    @SmallTest
    public void testSavesStateOnStop() throws Exception {
        _serviceCommand.execute(_app);

        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(GPSChangeState.State.START));
        _serviceCommand.onGPSChangeState(new GPSChangeState(GPSChangeState.State.STOP));

        verify(_mockDataStore, timeout(200).times(1)).commit();
    }

    @SmallTest
    public void testRegistersNmeaListenerOnStart() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);

        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(GPSChangeState.State.START));

        verify(_mockLocationManager, timeout(2000).times(1)).addNmeaListener(any(GpsStatus.NmeaListener.class));
    }

    @SmallTest
    public void testRemovesNmeaListenerOnStop() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);

        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(GPSChangeState.State.START));
        _serviceCommand.onGPSChangeState(new GPSChangeState(GPSChangeState.State.STOP));

        verify(_mockLocationManager, timeout(2000).times(1)).removeNmeaListener(any(GpsStatus.NmeaListener.class));
    }

    @SmallTest
    public void testRegistersSensorOnStart() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);

        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(GPSChangeState.State.START));

        verify(_mockSensorManager,timeout(2000).times(1)).registerListener(any(GPSSensorEventListener.class),any(Sensor.class),anyInt());
    }

    @SmallTest
    public void testRemovesSensorOnStop() throws Exception {
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);

        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(GPSChangeState.State.START));
        _serviceCommand.onGPSChangeState(new GPSChangeState(GPSChangeState.State.STOP));

        verify(_mockSensorManager,timeout(2000).times(1)).unregisterListener(any(GPSSensorEventListener.class));
    }

    @SmallTest
    public void testSetsPreferenceStartTimeOnStart() throws Exception {
        when(_mockTime.getCurrentTimeMilliseconds()).thenReturn((long)1000);
        when(_mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);

        _serviceCommand.execute(_app);
        _serviceCommand.onGPSChangeState(new GPSChangeState(GPSChangeState.State.START));

        verify(_mockDataStore,timeout(1000).times(1)).setStartTime(1000);
    }

    @SmallTest
    public void testsSendStateChangeToPebbleOnStart() throws Exception {
        //throw new Exception("State change not implemented");
    }
}
