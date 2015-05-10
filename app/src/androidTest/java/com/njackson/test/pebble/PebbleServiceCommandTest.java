package com.njackson.test.pebble;

import android.content.Context;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.njackson.application.modules.AndroidModule;
import com.njackson.application.modules.ForApplication;
import com.njackson.events.LiveServiceCommand.LiveMessage;
import com.njackson.events.PebbleServiceCommand.NewMessage;
import com.njackson.events.GPSServiceCommand.GPSStatus;
import com.njackson.events.GPSServiceCommand.NewLocation;
import com.njackson.events.base.BaseStatus;
import com.njackson.live.LiveTracking;
import com.njackson.pebble.PebbleServiceCommand;
import com.njackson.pebble.canvas.GPSData;
import com.njackson.pebble.canvas.ICanvasWrapper;
import com.njackson.test.application.TestApplication;
import com.njackson.pebble.IMessageManager;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;

import android.content.SharedPreferences;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

public class PebbleServiceCommandTest extends AndroidTestCase {

    @Inject Bus _bus;
    @Inject IMessageManager _mockMessageManager;
    @Inject SharedPreferences _mockPreferences;

    private PebbleServiceCommand _service;
    private GPSStatus _pebbleStatusEvent;
    private CountDownLatch _stateLatch;
    private PebbleServiceCommand _command;
    private static ICanvasWrapper _mockCanvasWrapper;
    private static TestApplication _app;

    @Module(
            includes = AndroidModule.class,
            injects = PebbleServiceCommandTest.class,
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

        @Provides
        ICanvasWrapper providesCanvasWrapper() { return _mockCanvasWrapper; }

        @Provides @Singleton @ForApplication
        Context provideApplicationContext() {
            return _app;
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

        _service = new PebbleServiceCommand();

        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());

        _app = new TestApplication();
        _app.setObjectGraph(ObjectGraph.create(TestModule.class));
        _app.inject(this);
        _bus.register(this);

        setupMocks();

        _command = new PebbleServiceCommand();

        _stateLatch = new CountDownLatch(1);
    }

    @Override
    public void tearDown() throws Exception {
        reset(_mockMessageManager);
        super.tearDown();
    }
    private void setupMocks() {
        _mockCanvasWrapper = mock(ICanvasWrapper.class);
        when(_mockPreferences.getBoolean("PREF_DEBUG", false)).thenReturn(true);
        when(_mockPreferences.getBoolean("LIVE_TRACKING", false)).thenReturn(true);
        when(_mockPreferences.getString("REFRESH_INTERVAL", "1000")).thenReturn("1000");
        when(_mockPreferences.getString("CANVAS_MODE", "disable")).thenReturn("canvas_and_pbw");
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
        _command.execute(_app);

        _bus.post(new GPSStatus(BaseStatus.Status.STARTED));
        Mockito.verify(_mockMessageManager,timeout(1000).times(1)).showWatchFace();
    }

    @SmallTest
    public void testUpdatePebbleGPSServiceStop() throws InterruptedException {
        _command.execute(_app);

        _bus.post(new GPSStatus(BaseStatus.Status.STOPPED));
        Mockito.verify(_mockMessageManager,timeout(1000).times(1)).offer(any(PebbleDictionary.class));
    }

    @SmallTest
    public void testUpdatePebbleOnNewLiveMessage() throws InterruptedException {
        _command.execute(_app);

        LiveMessage message = new LiveMessage();
        byte[] data = new byte[1 + LiveTracking.maxNumberOfFriend * LiveTracking.sizeOfAFriend];
        data[0] = 1; // numberOfFriends
        message.setLive(data);
        _bus.post(message);
        Mockito.verify(_mockMessageManager,timeout(1000).times(1)).offer(any(PebbleDictionary.class));
    }

    @SmallTest
    public void testSendsLocationToPebbleWhenNotCanvasOnly() throws InterruptedException {
        when(_mockPreferences.getString("CANVAS_MODE", "disable")).thenReturn("stuff");

        _command.execute(_app);

        ArgumentCaptor<PebbleDictionary> captor = new ArgumentCaptor<PebbleDictionary>();

        NewLocation event = new NewLocation();
        event.setUnits(0);
        event.setSpeed(45.4f);
        event.setTime(1420988759);

        _bus.post(event);

        verify(_mockMessageManager, timeout(2000).times(1)).offerIfLow(any(PebbleDictionary.class), anyInt());
    }

    @SmallTest
    public void testDoesNotSendsLocationToPebbleWhenCanvasOnly() throws InterruptedException {
        when(_mockPreferences.getString("CANVAS_MODE", "disable")).thenReturn("canvas_only");

        _command.execute(_app);

        ArgumentCaptor<PebbleDictionary> captor = new ArgumentCaptor<PebbleDictionary>();

        NewLocation event = new NewLocation();
        event.setUnits(0);
        event.setSpeed(45.4f);
        event.setTime(1420988759);

        _bus.post(event);

        verify(_mockMessageManager, timeout(2000).times(0)).offer(any(PebbleDictionary.class));
    }

    @SmallTest
    public void testSendsLocationToCanvasWhenNotCanvasDisabled() throws InterruptedException {
        when(_mockPreferences.getString("CANVAS_MODE", "disable")).thenReturn("stuff");

        _command.execute(_app);

        ArgumentCaptor<PebbleDictionary> captor = new ArgumentCaptor<PebbleDictionary>();

        NewLocation event = new NewLocation();
        event.setUnits(0);
        event.setSpeed(45.4f);
        event.setTime(1420988759);

        _bus.post(event);

        verify(_mockCanvasWrapper, timeout(2000).times(1)).set_gpsdata_details(any(GPSData.class), any(Context.class));
    }

    @SmallTest
    public void testDoesNotSendsLocationToCanvasWhenCanvasDisable() throws InterruptedException {
        when(_mockPreferences.getString("CANVAS_MODE", "disable")).thenReturn("disable");

        _command.execute(_app);

        ArgumentCaptor<PebbleDictionary> captor = new ArgumentCaptor<PebbleDictionary>();

        NewLocation event = new NewLocation();
        event.setUnits(0);
        event.setSpeed(45.4f);
        event.setTime(1420988759);

        _bus.post(event);

        verify(_mockCanvasWrapper, timeout(2000).times(0)).set_gpsdata_details(any(GPSData.class), any(Context.class));
    }

    @SmallTest
    public void testNewMessageEventSendsMessageToPebble() throws InterruptedException {
        _command.execute(_app);

        _bus.post(new NewMessage("A Message"));
        Mockito.verify(_mockMessageManager,timeout(1000).times(1)).showSimpleNotificationOnWatch(anyString(),anyString());
    }
}
