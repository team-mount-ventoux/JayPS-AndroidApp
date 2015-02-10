package com.njackson.test.advancedlocation;

import android.location.Location;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import fr.jayps.android.AdvancedLocation;

/**
 * Created by jay on 10/02/15.
 */
public class AdvancedLocationTest extends AndroidTestCase {

    @SmallTest
    public void testMaxSpeed() throws Exception {
        AdvancedLocation advancedLocation = new AdvancedLocation();

        advancedLocation.setMaxSpeed(13f);
        assertEquals(13f, advancedLocation.getMaxSpeed());

        Location location = new Location("PebbleBike");
        location.setAccuracy(35);
        location.setSpeed(28f);
        advancedLocation.onLocationChanged(location);

        assertEquals(13f, advancedLocation.getMaxSpeed());

        location.setAccuracy(5);
        location.setSpeed(22f);
        advancedLocation.onLocationChanged(location);

        assertEquals(22f, advancedLocation.getMaxSpeed());

        location.setSpeed(18f);
        advancedLocation.onLocationChanged(location);

        assertEquals(22f, advancedLocation.getMaxSpeed());

        location.setSpeed(45f);
        advancedLocation.onLocationChanged(location);

        assertEquals(45f, advancedLocation.getMaxSpeed());
    }
}
