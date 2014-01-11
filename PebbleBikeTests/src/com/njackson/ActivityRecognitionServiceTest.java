package com.njackson;

import android.content.Intent;
import android.os.IBinder;
import android.test.ServiceTestCase;

/**
 * Created by njackson on 11/01/2014.
 */
public class ActivityRecognitionServiceTest extends ServiceTestCase<ActivityRecognitionIntentService> {

    public ActivityRecognitionServiceTest() {
        super(ActivityRecognitionIntentService.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
     * The name 'test preconditions' is a convention to signal that if this
     * test doesn't pass, the test case was not set up properly and it might
     * explain any and all failures in other tests.  This is not guaranteed
     * to run before other tests, as junit uses reflection to find the tests.
     */
    public void testPreconditions() {
    }

    /**
     * Test basic startup/shutdown of Service
     */
    public void testStartable() {
        Intent startIntent = new Intent();
        startIntent.setClass(getContext(), ActivityRecognitionIntentService.class);
        startService(startIntent);
    }

    /**
     * Test binding to service
     */
    public void testBindable() {
        Intent startIntent = new Intent();
        startIntent.setClass(getContext(), ActivityRecognitionIntentService.class);
        IBinder service = bindService(startIntent);
    }

}
