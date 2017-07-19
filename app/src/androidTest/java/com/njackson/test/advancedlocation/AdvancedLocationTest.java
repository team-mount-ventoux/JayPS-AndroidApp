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

        Location location = new Location("JayPS");
        location.setAccuracy(35);
        location.setSpeed(28f);
        advancedLocation.onLocationChanged(location, 0, 0);

        assertEquals(13f, advancedLocation.getMaxSpeed());

        location.setAccuracy(5);
        location.setSpeed(22f);
        advancedLocation.onLocationChanged(location, 0, 0);

        assertEquals(22f, advancedLocation.getMaxSpeed());

        location.setSpeed(18f);
        advancedLocation.onLocationChanged(location, 0, 0);

        assertEquals(22f, advancedLocation.getMaxSpeed());

        location.setSpeed(45f);
        advancedLocation.onLocationChanged(location, 0, 0);

        assertEquals(45f, advancedLocation.getMaxSpeed());
    }

    @SmallTest
    public void testNbAscent() throws Exception {
        AdvancedLocation advancedLocation = new AdvancedLocation();
        advancedLocation.debugTagPrefix = "PB-";
        advancedLocation.debugLevel = 2;

        advancedLocation.setNbAscent(5);
        assertEquals(5, advancedLocation.getNbAscent());

        Location location = new Location("JayPS");
        long time = 1000000l;
        double latitude = 0;
        location.setAccuracy(AdvancedLocation.MAX_ACCURACY_FOR_NB_ASCENT+1);
        location.setAltitude(250);
        location.setTime(time); time += 30000; location.setLatitude(latitude); latitude += 0.001;
        advancedLocation.onLocationChanged(location, 0, 0);
        assertEquals(5, advancedLocation.getNbAscent());

        location.setAccuracy(AdvancedLocation.MAX_ACCURACY_FOR_NB_ASCENT-1);
        location.setAltitude(250);
        location.setTime(time); time += 30000; location.setLatitude(latitude); latitude += 0.001;
        advancedLocation.onLocationChanged(location, 0, 0);
        assertEquals(5, advancedLocation.getNbAscent());

        location.setAltitude(250);
        location.setTime(time); time += 30000; location.setLatitude(latitude); latitude += 0.001;
        advancedLocation.onLocationChanged(location, 0, 0);
        assertEquals(5, advancedLocation.getNbAscent());

        location.setAltitude(250+AdvancedLocation.NB_ASCENT_DELTA_ALTITUDE-1);
        location.setTime(time); time += 30000; location.setLatitude(latitude); latitude += 0.001;
        advancedLocation.onLocationChanged(location, 0, 0);
        assertEquals(5, advancedLocation.getNbAscent());

        location.setAltitude(450);
        location.setTime(time); time += 30000; location.setLatitude(latitude); latitude += 0.001;
        advancedLocation.onLocationChanged(location, 0, 0);
        assertEquals(6, advancedLocation.getNbAscent());

        double min = 20;
        location.setAltitude(min);
        location.setTime(time); time += 30000; location.setLatitude(latitude); latitude += 0.001;
        advancedLocation.onLocationChanged(location, 0, 0);
        assertEquals(6, advancedLocation.getNbAscent());

        location.setAltitude(min+AdvancedLocation.NB_ASCENT_DELTA_ALTITUDE-5);
        location.setTime(time); time += 30000; location.setLatitude(latitude); latitude += 0.001;
        advancedLocation.onLocationChanged(location, 0, 0);
        assertEquals(6, advancedLocation.getNbAscent());

        location.setAltitude(min+AdvancedLocation.NB_ASCENT_DELTA_ALTITUDE-150);
        location.setTime(time); time += 30000; location.setLatitude(latitude); latitude += 0.001;
        advancedLocation.onLocationChanged(location, 0, 0);
        assertEquals(6, advancedLocation.getNbAscent());

        location.setAltitude(min+AdvancedLocation.NB_ASCENT_DELTA_ALTITUDE+3);
        location.setTime(time); time += 30000; location.setLatitude(latitude); latitude += 0.001;
        advancedLocation.onLocationChanged(location, 0, 0);
        assertEquals(7, advancedLocation.getNbAscent());
    }
}
