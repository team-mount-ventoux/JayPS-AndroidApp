package com.njackson.test.live;

import android.content.Intent;
import com.njackson.application.modules.PebbleBikeModule;
import com.njackson.events.GPSService.CurrentState;
import com.njackson.events.GPSService.NewLocation;
import com.njackson.live.LiveService;
import com.njackson.test.application.TestApplication;
import com.njackson.virtualpebble.IMessageManager;
import com.njackson.virtualpebble.PebbleService;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import static org.mockito.Mockito.*;

import android.content.SharedPreferences;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

public class LiveServiceTest extends ServiceTestCase<PebbleService>{

    private static final String TAG = "PB-LiveServiceTest";

    @Inject Bus _bus;
    @Inject IMessageManager _mockMessageManager;
    @Inject SharedPreferences _mockPreferences;

    private PebbleService _service;
    private TestApplication _app;
    //private CurrentState _pebbleStatusEvent;
    //private CountDownLatch _stateLatch;

    public LiveServiceTest() {
        super(PebbleService.class);
    }

    @Module(
            includes = PebbleBikeModule.class,
            injects = LiveServiceTest.class,
            overrides = true,
            complete = false
    )
    static class TestModule {
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
    }
    @Subscribe
    public void onNewLocationEvent(NewLocation newLocation) {
        if (_mockPreferences.getBoolean("LIVE_TRACKING", false)) {
            Log.i(TAG, "onNewLocationEvent time=" + newLocation.getTime());
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        _service = new PebbleService();

        System.setProperty("dexmaker.dexcache", getSystemContext().getCacheDir().getPath());

        _app = new TestApplication();
        _app.setObjectGraph(ObjectGraph.create(TestModule.class));
        _app.inject(this);
        _bus.register(this);

        setApplication(_app);

        setupMocks();

        //_stateLatch = new CountDownLatch(1);
    }

    @Override
    public void tearDown() throws Exception {
        reset(_mockMessageManager);
        super.tearDown();
    }
    private void setupMocks() {
        when(_mockPreferences.getBoolean("LIVE_TRACKING", false)).thenReturn(true);
        when(_mockPreferences.getBoolean("PREF_DEBUG", false)).thenReturn(true);
        when(_mockPreferences.getString("LIVE_TRACKING_LOGIN", "")).thenReturn("test");
        when(_mockPreferences.getString("LIVE_TRACKING_PASSWORD", "")).thenReturn("test");
        when(_mockPreferences.getString("LIVE_TRACKING_URL", "")).thenReturn("url");
        when(_mockPreferences.getString("LIVE_TRACKING_MMT_LOGIN", "")).thenReturn("test");
        when(_mockPreferences.getString("LIVE_TRACKING_MMT_PASSWORD", "")).thenReturn("test");
        when(_mockPreferences.getString("LIVE_TRACKING_MMT_URL", "")).thenReturn("url");
    }

    private void startService() throws InterruptedException {
        Intent startIntent = new Intent(getSystemContext(), LiveService.class);
        startService(startIntent);
        _service = getService();

        //_stateLatch.await(2000, TimeUnit.MILLISECONDS);
    }
    @SmallTest
    public void testEventOnNewLocationEvent() throws Exception {

        startService();

        NewLocation event = new NewLocation();
        event.setUnits(0);
        event.setSpeed(45.4f);
        event.setTime(1420988759);

        _bus.post(event);
    }
}
