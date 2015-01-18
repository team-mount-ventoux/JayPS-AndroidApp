package com.njackson.test.pebble;

import android.content.Intent;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.njackson.Constants;
import com.njackson.adapters.NewLocationToPebbleDictionary;
import com.njackson.application.modules.PebbleBikeModule;
import com.njackson.events.PebbleService.NewMessage;
import com.njackson.events.status.GPSStatus;
import com.njackson.events.GPSService.NewLocation;
import com.njackson.test.application.TestApplication;
import com.njackson.pebble.IMessageManager;
import com.njackson.pebble.PebbleService;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;

import android.content.SharedPreferences;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

public class PebbleServiceTest extends ServiceTestCase<PebbleService>{

    @Inject Bus _bus;
    @Inject IMessageManager _mockMessageManager;
    @Inject SharedPreferences _mockPreferences;

    private PebbleService _service;
    private TestApplication _app;
    private GPSStatus _pebbleStatusEvent;
    private CountDownLatch _stateLatch;

    public PebbleServiceTest() {
        super(PebbleService.class);
    }

    @Module(
            includes = PebbleBikeModule.class,
            injects = PebbleServiceTest.class,
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
    public void onPebbleServiceStatusEvent(GPSStatus event) {
        _pebbleStatusEvent = event;

        _stateLatch.countDown();
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

        _stateLatch = new CountDownLatch(1);
    }

    @Override
    public void tearDown() throws Exception {
        reset(_mockMessageManager);
        super.tearDown();
    }
    private void setupMocks() {
        when(_mockPreferences.getBoolean("PREF_DEBUG", false)).thenReturn(true);
        when(_mockPreferences.getBoolean("LIVE_TRACKING", false)).thenReturn(true);
        when(_mockPreferences.getString("REFRESH_INTERVAL", "1000")).thenReturn("1000");
    }

    private void startService() throws InterruptedException {
        Intent startIntent = new Intent(getSystemContext(), PebbleService.class);
        startService(startIntent);
        _service = getService();

        _stateLatch.await(2000, TimeUnit.MILLISECONDS);
    }
/*
    @SmallTest
    public void testServiceHideWatchFaceOnStop() throws InterruptedException {
        startService();
        shutdownService();
        Mockito.verify(_mockMessageManager,times(1)).hideWatchFace();
    }
*/
    @SmallTest
    public void testServiceShowsWatchFaceOnGPSServiceStart() throws InterruptedException {
        startService();
        _bus.post(new GPSStatus(GPSStatus.State.STARTED));
        Mockito.verify(_mockMessageManager,timeout(1000).times(1)).showWatchFace();
    }

    @SmallTest
    public void testServiceRespondsToNewGPSLocation() throws InterruptedException {
        startService();

        ArgumentCaptor<PebbleDictionary> captor = new ArgumentCaptor<PebbleDictionary>();

        NewLocation event = new NewLocation();
        event.setUnits(0);
        event.setSpeed(45.4f);
        event.setTime(1420988759);

        _bus.post(event);

        verify(_mockMessageManager, timeout(2000).times(1)).offer(any(PebbleDictionary.class));
    }

    @SmallTest
    public void testNewMessageEventSendsMessageToPebble() throws InterruptedException {
        startService();
        _bus.post(new NewMessage("A Message"));
        Mockito.verify(_mockMessageManager,timeout(1000).times(1)).showSimpleNotificationOnWatch(anyString(),anyString());
    }
}
