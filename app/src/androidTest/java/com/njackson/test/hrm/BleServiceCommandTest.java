package com.njackson.test.hrm;

import com.njackson.application.modules.AndroidModule;
import com.njackson.application.modules.ForApplication;
import com.njackson.events.BleServiceCommand.BleStatus;
import com.njackson.sensor.BLEServiceCommand;
import com.njackson.sensor.IBle;
import com.njackson.test.application.TestApplication;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import static org.mockito.Mockito.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.AndroidTestCase;

import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

public class BleServiceCommandTest extends AndroidTestCase {
    private CountDownLatch _stateLatch;

    @Inject Bus _bus;
    @Inject SharedPreferences _mockPreferences;

    private BleStatus _state;
    private BLEServiceCommand _service;
    private TestApplication _app;

    @Module(
            includes = AndroidModule.class,
            injects = BleServiceCommandTest.class,
            overrides = true,
            complete = false
    )
    class TestModule {
        @Provides
        @Singleton
        SharedPreferences provideSharedPreferences() {
            return mock(SharedPreferences.class);
        }

        @Provides @Singleton
        IBle provideHrm() { return mock(IBle.class); }

        @Provides @Singleton @ForApplication
        Context provideApplicationContext() {
            return getContext();
        }
    }
    @Subscribe
    public void onChangeStateEvent(BleStatus state) {
        _state = state;
        _stateLatch.countDown();
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

        _stateLatch = new CountDownLatch(1);
        _service = new BLEServiceCommand();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }
    private void setupMocks() {
        when(_mockPreferences.getString("hrm_address", "")).thenReturn("12");
        when(_mockPreferences.getString("hrm_address2", "")).thenReturn("34");
        when(_mockPreferences.getString("hrm_address3", "")).thenReturn("56");
    }

// we no longer start HrmServiceCommand if hrm_address is not setted
//    @SmallTest
//    public void testOnStartSendsServiceUnableToStartWhenDisable() throws Exception {
//        when(_mockPreferences.getString("hrm_address", "")).thenReturn("");
//        _service.execute(_app);
//
//        _bus.post(new GPSStatus(BaseStatus.Status.STARTED));
//        _stateLatch.await(1000, TimeUnit.MILLISECONDS);
//
//        assertEquals(BaseStatus.Status.UNABLE_TO_START, _state.getStatus());
//    }
/*
    disabled because android.os.Build.VERSION.SDK_INT doesn't return the right value in the tests
    @SmallTest
    public void testOnStartSendsServiceStartedWhenEnable() throws Exception {
        when(_mockPreferences.getString("hrm_address", "")).thenReturn("12");
        _service.execute(_app);

        _bus.post(new GPSStatus(BaseStatus.Status.STARTED));
        _stateLatch.await(1000, TimeUnit.MILLISECONDS);

        assertEquals(BaseStatus.Status.STARTED, _state.getStatus());
    }
*/
/*
    disabled because android.os.Build.VERSION.SDK_INT doesn't return the right value in the tests
    @SmallTest
    public void testOnStopSendsServiceStopped() throws Exception {
        when(_mockPreferences.getString("hrm_address", "")).thenReturn("12");
        _service.execute(_app);

        // need to start the service before stopping
        _bus.post(new GPSStatus(BaseStatus.Status.STARTED));
        _stateLatch.await(1000, TimeUnit.MILLISECONDS);

        _stateLatch = new CountDownLatch(1);
        _bus.post(new GPSStatus(BaseStatus.Status.STOPPED));
        _stateLatch.await(1000, TimeUnit.MILLISECONDS);

        assertEquals(BaseStatus.Status.STOPPED, _state.getStatus());
    }
    */
}
