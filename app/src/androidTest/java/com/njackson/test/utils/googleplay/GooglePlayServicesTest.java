package com.njackson.test.utils.googleplay;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.utils.googleplay.GooglePlayServices;

/**
 * Created by njackson on 08/01/15.
 */
public class GooglePlayServicesTest extends AndroidTestCase {

    private GooglePlayServices _utils;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        _utils = new GooglePlayServices();
    }

    @SmallTest
    public void testGeneratesSessionIdentifierCorrectly() {
        long time = 1000454;
        String identifier = _utils.generateSessionIdentifier(time);

        assertEquals("PebbleBike-1000454", identifier);
    }

    @SmallTest
    public void testGeneratesSessionNameCorrectly() {
        String name = _utils.generateSessionName();

        assertEquals("Pebble Bike", name);
    }

}
