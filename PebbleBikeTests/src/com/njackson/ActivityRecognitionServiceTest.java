package com.njackson;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.test.ServiceTestCase;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

/**
 * Created by njackson on 11/01/2014.
 */
public class ActivityRecognitionServiceTest extends ServiceTestCase<ActivityRecognitionIntentService> {

    private BroadcastReceiver receiver = null;
    private Bundle results = null;

    public ActivityRecognitionServiceTest() {
        super(ActivityRecognitionIntentService.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                results = intent.getExtras();
            }
        };
        IntentFilter filter =  new IntentFilter();
        filter.addAction("com.njackson.intent.action.MESSAGE_PROCESSED");
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        getSystemContext().registerReceiver(receiver, filter);
    }

    public void testRecieveBicycle() throws InterruptedException {
        startWithType(DetectedActivity.ON_BICYCLE);
        assertNotNull(results);
        assertEquals(results.getInt("ACTIVITY_CHANGED"),DetectedActivity.ON_BICYCLE);
    }

    public void testRecieveOnFoot() throws InterruptedException {
        startWithType(DetectedActivity.ON_FOOT);
        assertNotNull(results);
        assertEquals(results.getInt("ACTIVITY_CHANGED"),DetectedActivity.ON_FOOT);
    }

    private void startWithType(int activityType) throws InterruptedException {
        DetectedActivity activity = new DetectedActivity(activityType,1);
        ActivityRecognitionResult result = new ActivityRecognitionResult(activity,1000,1000);

        Intent startIntent = new Intent();
        startIntent.putExtra(ActivityRecognitionResult.EXTRA_ACTIVITY_RESULT,result);
        startIntent.setClass(getContext(), ActivityRecognitionIntentService.class);

        getSystemContext().startService(startIntent);
        Thread.sleep(100);
    }

    @Override
    protected  void tearDown() throws Exception {
        super.tearDown();
        if(receiver != null)
            getSystemContext().unregisterReceiver(receiver);
    } 
}
