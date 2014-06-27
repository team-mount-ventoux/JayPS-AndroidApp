package com.njackson.test.events.GPSService;

import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.events.GPSService.NewAltitiude;

import junit.framework.TestCase;

/**
 * Created by server on 11/04/2014.
 */
public class NewAltitudeEventTest extends TestCase {

    @SmallTest
    public void test_create_NewAltitudeEvent_with_more_than_14_values_throws_eception() {
        Exception exception = null;

        try {
            NewAltitiude event = new NewAltitiude(new float[15], 2323);
        }catch (Exception e) {
            exception = e;
        }

        assertNotNull("Expected event to be thrown",exception);
    }

    @SmallTest
    public void test_create_NewAltitudeEvent_with_less_than_14_values_throws_eception() {
        Exception exception = null;

        try {
            NewAltitiude event = new NewAltitiude(new float[13], 2323);
        }catch (Exception e) {
            exception = e;
        }

        assertNotNull("Expected event to be thrown",exception);
    }

    @SmallTest
    public void test_create_NewAltitudeEvent_null_values_throws_eception() {
        Exception exception = null;

        try {
            NewAltitiude event = new NewAltitiude(null, 2323);
        }catch (Exception e) {
            exception = e;
        }

        assertNotNull("Expected event to be thrown",exception);
    }

    @SmallTest
    public void test_create_NewAltitudeEvent_with_14_values_does_not_throws_eception() {
        Exception exception = null;

        try {
            NewAltitiude event = new NewAltitiude(new float[14], 2323);
        }catch (Exception e) {
            exception = e;
        }

        assertNull("Expected no event to be thrown",exception);
    }

}
