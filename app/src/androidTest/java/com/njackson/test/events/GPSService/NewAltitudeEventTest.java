package com.njackson.test.events.GPSService;

import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.events.GPSServiceCommand.NewAltitude;

import junit.framework.TestCase;

/**
 * Created by server on 11/04/2014.
 */
public class NewAltitudeEventTest extends TestCase {

    @SmallTest
    public void test_create_NewAltitudeEvent_with_more_than_14_values_throws_eception() {
        Exception exception = null;

        try {
            NewAltitude event = new NewAltitude(new int[15]);
        }catch (Exception e) {
            exception = e;
        }

        assertNotNull("Expected event to be thrown",exception);
    }

    @SmallTest
    public void test_create_NewAltitudeEvent_with_less_than_14_values_throws_eception() {
        Exception exception = null;

        try {
            NewAltitude event = new NewAltitude(new int[13]);
        }catch (Exception e) {
            exception = e;
        }

        assertNotNull("Expected event to be thrown",exception);
    }

    @SmallTest
    public void test_create_NewAltitudeEvent_null_values_throws_eception() {
        Exception exception = null;

        try {
            NewAltitude event = new NewAltitude(null);
        }catch (Exception e) {
            exception = e;
        }

        assertNotNull("Expected event to be thrown",exception);
    }

    @SmallTest
    public void test_create_NewAltitudeEvent_with_14_values_does_not_throws_eception() {
        Exception exception = null;

        try {
            NewAltitude event = new NewAltitude(new int[14]);
        }catch (Exception e) {
            exception = e;
        }

        assertNull("Expected no event to be thrown",exception);
    }

}
