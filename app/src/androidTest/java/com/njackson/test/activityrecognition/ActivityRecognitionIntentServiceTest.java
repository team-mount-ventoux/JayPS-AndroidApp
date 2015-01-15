package com.njackson.activityrecognition;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.test.ServiceTestCase;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.njackson.application.modules.PebbleBikeModule;
import com.njackson.events.ActivityRecognitionService.NewActivityEvent;
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
 * Created by njackson on 11/01/2014.
 */
public class ActivityRecognitionIntentServiceTest extends ServiceTestCase<ActivityRecognitionIntentService> {

    private TestApplication _app;

    public ActivityRecognitionIntentServiceTest() {
        super(ActivityRecognitionIntentService.class);
    }

    NewActivityEvent _event;
    CountDownLatch _latch;

    @Inject Bus _bus;

    @Module(
            includes = PebbleBikeModule.class,
            injects = ActivityRecognitionIntentServiceTest.class,
            overrides = true,
            complete = false
    )
    static class TestModule {

    }

    @Subscribe
    public void newActivityEvent(NewActivityEvent event) {
        _event = event;
        _latch.countDown();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        _app = new TestApplication();
        _app.setObjectGraph(ObjectGraph.create(TestModule.class));
        _app.inject(this);
        _bus.register(this);

        setApplication(_app);

        _latch = new CountDownLatch(1);
    }

    public void testRecieveBicycle() throws InterruptedException {
        startWithType(DetectedActivity.ON_BICYCLE);

        _latch.await(2000, TimeUnit.MILLISECONDS);
        assertEquals(_event.getActivityType(), DetectedActivity.ON_BICYCLE);
    }

    public void testRecieveRunning() throws InterruptedException {
        startWithType(DetectedActivity.RUNNING);

        _latch.await(2000, TimeUnit.MILLISECONDS);
        assertEquals(_event.getActivityType(), DetectedActivity.RUNNING);
    }

    public void testRecieveWalking() throws InterruptedException {
        startWithType(DetectedActivity.WALKING);

        _latch.await(2000, TimeUnit.MILLISECONDS);
        assertEquals(_event.getActivityType(), DetectedActivity.WALKING);
    }

    public void testRecieveStill() throws InterruptedException {
        startWithType(DetectedActivity.STILL);

        _latch.await(2000, TimeUnit.MILLISECONDS);
        assertEquals(_event.getActivityType(), DetectedActivity.STILL);
    }

    private void startWithType(int activityType) throws InterruptedException {
        DetectedActivity activity = new DetectedActivity(activityType,1);
        ActivityRecognitionResult result = new ActivityRecognitionResult(activity,1000,1000);

        Intent startIntent = new Intent();
        startIntent.putExtra(ActivityRecognitionResult.EXTRA_ACTIVITY_RESULT,result);
        startIntent.setClass(getSystemContext(), ActivityRecognitionIntentService.class);

        startService(startIntent);
    }
}