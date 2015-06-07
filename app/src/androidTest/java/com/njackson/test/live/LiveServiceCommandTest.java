package com.njackson.test.live;

import com.njackson.application.modules.AndroidModule;
import com.njackson.events.GPSServiceCommand.NewLocation;
import com.njackson.events.LiveServiceCommand.LiveChangeState;
import com.njackson.events.base.BaseChangeState;
import com.njackson.live.ILiveTracking;
import com.njackson.live.LiveServiceCommand;
import com.njackson.test.application.TestApplication;
import com.njackson.pebble.IMessageManager;

import com.squareup.otto.Bus;

import static org.mockito.Mockito.*;

import android.content.SharedPreferences;
import android.location.Location;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

public class LiveServiceCommandTest extends AndroidTestCase {

    @Inject Bus _bus;
    @Inject IMessageManager _mockMessageManager;
    @Inject SharedPreferences _mockPreferences;

    private LiveServiceCommand _service;
    private TestApplication _app;

    private static ILiveTracking _mockLiveTrackingJay;
    private static ILiveTracking _mockLiveTrackingMmt;

    @Module(
            includes = AndroidModule.class,
            injects = LiveServiceCommandTest.class,
            overrides = true,
            complete = false
    )
    class TestModule {
        @Provides
        @Singleton
        public IMessageManager providesMessageManager() {
            return mock(IMessageManager.class);
        }

        @Provides
        @Singleton
        SharedPreferences provideSharedPreferences() {
            return mock(SharedPreferences.class);
        }

        @Provides
        @Named("LiveTrackingJayPS")
        ILiveTracking provideLiveTrackingJay() {
            return _mockLiveTrackingJay;
        }

        @Provides
        @Named("LiveTrackingMmt")
        ILiveTracking provideLiveTrackingMmt() {
            return _mockLiveTrackingMmt;
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());

        _app = new TestApplication();
        _app.setObjectGraph(ObjectGraph.create(new TestModule()));
        _app.inject(this);
        _bus.register(this);

        setupMocks();

        _service = new LiveServiceCommand();
    }

    @Override
    public void tearDown() throws Exception {
        reset(_mockMessageManager);
        super.tearDown();
    }
    private void setupMocks() {
        _mockLiveTrackingJay = mock(ILiveTracking.class);
        _mockLiveTrackingMmt = mock(ILiveTracking.class);
    }

    @SmallTest
    public void testOnNewLocationEventAddsPointWhenEnabledAndTimeNot0() throws Exception {
        when(_mockPreferences.getBoolean("LIVE_TRACKING", false)).thenReturn(true);
        _service.execute(_app);

        NewLocation event = new NewLocation();
        event.setTime(1000);
        _bus.post(event);

        verify(_mockLiveTrackingJay,timeout(2000).times(1)).addPoint(any(Location.class), any(Location.class), anyInt(), anyInt());
    }

    @SmallTest
    public void testOnNewLocationEventNotAddsPointWhenEnabledAndTime0() throws Exception {
        when(_mockPreferences.getBoolean("LIVE_TRACKING", false)).thenReturn(true);
        _service.execute(_app);

        NewLocation event = new NewLocation();
        event.setTime(0);
        _bus.post(event);

        verify(_mockLiveTrackingJay,timeout(2000).times(0)).addPoint(any(Location.class), any(Location.class), anyInt(), anyInt());
    }

    @SmallTest
    public void testOnNewLocationEventNotAddsPointWhenDisabled() throws Exception {
        when(_mockPreferences.getBoolean("LIVE_TRACKING", false)).thenReturn(false);
        _service.execute(_app);

        NewLocation event = new NewLocation();
        _bus.post(event);

        verify(_mockLiveTrackingJay,timeout(2000).times(0)).addPoint(any(Location.class), any(Location.class), anyInt(), anyInt());
    }

    @SmallTest
    public void testChangeStateStartSetsLoginJay() {
        when(_mockPreferences.getString("LIVE_TRACKING_LOGIN", "")).thenReturn("jaypslogin");
        _service.execute(_app);

        _bus.post(new LiveChangeState(BaseChangeState.State.START));

        verify(_mockLiveTrackingJay,timeout(2000).times(1)).setLogin("jaypslogin");
    }

    @SmallTest
    public void testChangeStateStartSetsPasswordJay() {
        when(_mockPreferences.getString("LIVE_TRACKING_PASSWORD", "")).thenReturn("jaypspassword");
        _service.execute(_app);

        _bus.post(new LiveChangeState(BaseChangeState.State.START));

        verify(_mockLiveTrackingJay,timeout(2000).times(1)).setPassword("jaypspassword");
    }

    @SmallTest
    public void testChangeStateStartSetsUrlJay() {
        when(_mockPreferences.getString("LIVE_TRACKING_URL", "")).thenReturn("jaypsurl");
        _service.execute(_app);

        _bus.post(new LiveChangeState(BaseChangeState.State.START));

        verify(_mockLiveTrackingJay,timeout(2000).times(1)).setUrl("jaypsurl");
    }

    @SmallTest
    public void testChangeStateStartSetsLoginMmt() {
        when(_mockPreferences.getString("LIVE_TRACKING_MMT_LOGIN", "")).thenReturn("mmtlogin");
        _service.execute(_app);

        _bus.post(new LiveChangeState(BaseChangeState.State.START));

        verify(_mockLiveTrackingMmt,timeout(2000).times(1)).setLogin("mmtlogin");
    }

    @SmallTest
    public void testChangeStateStartSetsPasswordMmt() {
        when(_mockPreferences.getString("LIVE_TRACKING_MMT_PASSWORD", "")).thenReturn("mmtpassword");
        _service.execute(_app);

        _bus.post(new LiveChangeState(BaseChangeState.State.START));

        verify(_mockLiveTrackingMmt,timeout(2000).times(1)).setPassword("mmtpassword");
    }

    @SmallTest
    public void testChangeStateStartSetsUrlMmt() {
        when(_mockPreferences.getString("LIVE_TRACKING_MMT_URL", "")).thenReturn("mmturl");
        _service.execute(_app);

        _bus.post(new LiveChangeState(BaseChangeState.State.START));

        verify(_mockLiveTrackingMmt,timeout(2000).times(1)).setUrl("mmturl");
    }


}
