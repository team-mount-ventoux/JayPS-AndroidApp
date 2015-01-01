package com.njackson.test.activityrecognition;

import android.content.Intent;
import android.os.IBinder;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.activityrecognition.ActivityRecognitionService;
import com.njackson.application.modules.PebbleBikeModule;
import com.njackson.events.GPSService.CurrentState;
import com.njackson.gps.GPSService;
import com.njackson.test.application.TestApplication;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.Module;
import dagger.ObjectGraph;

/**
 * Created by njackson on 01/01/15.
 */
public class ActivityRecognitionServiceTest extends ServiceTestCase<ActivityRecognitionService> {

    @Inject Bus _bus = new Bus();

    private CurrentState _activityStatusEvent;
    private CountDownLatch _stateLatch;
    private ActivityRecognitionService _service;

    @Module(
            includes = PebbleBikeModule.class,
            injects = ActivityRecognitionServiceTest.class,
            overrides = true,
            complete = false
    )
    static class TestModule {
    }

    /**
     * Constructor
     *
     * @param serviceClass The type of the service under test.
     */
    public ActivityRecognitionServiceTest(Class<ActivityRecognitionService> serviceClass) {
        super(serviceClass);
    }

    public ActivityRecognitionServiceTest() {
        super(ActivityRecognitionService.class);
    }

    @Subscribe
    public void onGPSStatusEvent(CurrentState event) {
        _activityStatusEvent = event;
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

        _stateLatch = new CountDownLatch(1);
    }

    private void startService() throws Exception {
        Intent startIntent = new Intent(getSystemContext(), ActivityRecognitionService.class);
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
