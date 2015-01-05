package com.njackson.test.fit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.IBinder;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.application.modules.PebbleBikeModule;
import com.njackson.events.GoogleFitService.CurrentState;
import com.njackson.fit.GoogleFitService;
import com.njackson.gps.GPSService;
import com.njackson.test.application.TestApplication;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import static org.mockito.Mockito.mock;

/**
 * Created by njackson on 05/01/15.
 */
public class GoogleFitServiceTest extends ServiceTestCase<GoogleFitService> {
    private GoogleFitService _service;
    private CountDownLatch _stateLatch;

    @Inject Bus _bus;
    private CurrentState _state;

    @Module(
            includes = PebbleBikeModule.class,
            injects = GoogleFitServiceTest.class,
            overrides = true,
            complete = false
    )
    static class TestModule {

    }


    @Subscribe
    public void onChangeStateEvent(CurrentState state) {
        _state = state;
        _stateLatch.countDown();
    }

    /**
     * Constructor
     *
     * @param serviceClass The type of the service under test.
     */
    public GoogleFitServiceTest(Class<GoogleFitService> serviceClass) {
        super(serviceClass);
    }

    public GoogleFitServiceTest() {
        super(GoogleFitService.class);
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

        _stateLatch = new CountDownLatch(1);
    }

    private void startService() throws Exception {
        Intent startIntent = new Intent(getSystemContext(), GPSService.class);
        startService(startIntent);
        _service = getService();
        _stateLatch.await(2000, TimeUnit.MILLISECONDS);
    }

    @SmallTest
    public void testOnBindReturnsNull() throws Exception {
        startService();
        IBinder binder = _service.onBind(new Intent());

        assertNull(binder);
    }
}
