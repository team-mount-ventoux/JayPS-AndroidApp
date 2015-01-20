package com.njackson.test.utils.googleplay;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.location.DetectedActivity;
import com.njackson.adapters.DetectedToFitnessActivityAdapater;

/**
 * Created by njackson on 14/01/15.
 */
public class DetectedToFitnessActivityAdapaterTest extends AndroidTestCase{

    private int[] detectedActivities = {DetectedActivity.STILL, DetectedActivity.RUNNING, DetectedActivity.WALKING, DetectedActivity.ON_FOOT, DetectedActivity.ON_BICYCLE};
    private String[] fitnessActivities = {FitnessActivities.STILL, FitnessActivities.RUNNING, FitnessActivities.WALKING, FitnessActivities.ON_FOOT, FitnessActivities.BIKING};

    @SmallTest
    public void testConvertsCorrectly() {
        for(int n=0; n < detectedActivities.length; n ++ ){
            DetectedToFitnessActivityAdapater adapter = new DetectedToFitnessActivityAdapater(detectedActivities[n]);
            assertEquals(fitnessActivities[n], adapter.getActivity());
        }
    }

}
