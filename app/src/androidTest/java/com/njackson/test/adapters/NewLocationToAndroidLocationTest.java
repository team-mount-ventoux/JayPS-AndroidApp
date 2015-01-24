package com.njackson.test.adapters;

import android.location.Location;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.adapters.NewLocationToAndroidLocation;
import com.njackson.events.GPSServiceCommand.NewLocation;

/**
 * Created by njackson on 24/01/15.
 */
public class NewLocationToAndroidLocationTest extends AndroidTestCase {

    @SmallTest
    public void testSetsProvider() {
        NewLocation newLocation = new NewLocation();
        newLocation.setAccuracy(102);

        Location location = new NewLocationToAndroidLocation("Test", newLocation);

        assertEquals("Test", location.getProvider());
    }

    @SmallTest
    public void testSetsAccuracy() {
        NewLocation newLocation = new NewLocation();
        newLocation.setAccuracy(102);

        Location location = new NewLocationToAndroidLocation("Test", newLocation);

        assertEquals(102.0, location.getAccuracy());
    }

    @SmallTest
    public void testSetsLatitude() {
        NewLocation newLocation = new NewLocation();
        newLocation.setLatitude(103);

        Location location = new NewLocationToAndroidLocation("Test", newLocation);

        assertEquals(103.0, location.getLatitude());
    }

    @SmallTest
    public void testSetsLongitude() {
        NewLocation newLocation = new NewLocation();
        newLocation.setLongitude(104);

        Location location = new NewLocationToAndroidLocation("Test", newLocation);

        assertEquals(104.0, location.getLongitude());
    }

    @SmallTest
    public void testSetsTime() {
        NewLocation newLocation = new NewLocation();
        newLocation.setTime(105);

        Location location = new NewLocationToAndroidLocation("Test", newLocation);

        assertEquals(105, location.getTime());
    }

}
