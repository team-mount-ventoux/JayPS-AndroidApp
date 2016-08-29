package com.njackson.test.adapters;

import android.test.AndroidTestCase;

import com.getpebble.android.kit.util.PebbleDictionary;
import com.njackson.Constants;
import com.njackson.adapters.NewLocationToPebbleDictionary;
import com.njackson.events.GPSServiceCommand.NewLocation;
import com.njackson.gps.Navigator;

/**
 * Created by server on 25/03/2014.
 */
public class NewLocationToPebbleDataTest extends AndroidTestCase{

    byte[] data;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        NewLocation event = new NewLocation();
        event.setUnits(1);
        event.setBearing(190.1);
        event.setYpos(2.2);
        event.setXpos(3.3);
        event.setElapsedTimeSeconds(423);
        event.setAvgSpeed(5.5f);
        event.setAccuracy(6.6f);
        event.setAltitude(700.7);
        event.setAscent(38.8);
        event.setAscentRate(9.9f);
        event.setDistance(10.1f);
        event.setLatitude(11.1);
        event.setLongitude(12.2);
        event.setSlope(13.3f);
        event.setSpeed(14.4f);
        event.setHeartRate(123);
        event.setCyclingCadence(134);

        Navigator navigator = new Navigator();
        PebbleDictionary dic = new NewLocationToPebbleDictionary(event, navigator, true, true, true, 5000, Constants.MIN_VERSION_PEBBLE_FOR_LOCATION_DATA_V3, true);
        data = dic.getBytes(Constants.PEBBLE_LOCATION_DATA_V3);

        assertNotNull("Data should not be null",data);
    }

    public void testConvertsUnitsCorrectly() {
        assertEquals("Expected units bit to be true",true,bitIsSet(data[0],0));
    }

    public void testServiceRunningCorrectly() {
        assertEquals("Expected service running bit to be true",true,bitIsSet(data[0], 3));
    }

    public void testDebugCorrectly() {
        assertEquals("Expected debug bit to be true",true,bitIsSet(data[0],4));
    }

    public void testLiveTracking() {
        assertEquals("Expected live tracking bit to be true",true,bitIsSet(data[0],5));
    }

    public void testRefresh() {
        assertEquals("Expected refresh of 3",3,((data[0] >> 6) + 4) % 4); // +4 to avoid negative numbers after >> 6
    }

    public void testAccuracy() {
        assertEquals("Accuracy: Expected value 7",7,data[NewLocationToPebbleDictionary.BYTE_ACCURACY]);
    }

    public void testDistance1() {
        assertEquals("Distance 1: Expected value 1",-14,data[NewLocationToPebbleDictionary.BYTE_DISTANCE1]);
    }

    public void testDistance2() {
        assertEquals("Distance 2: Expected value 3",3,data[NewLocationToPebbleDictionary.BYTE_DISTANCE2]);
    }

    public void testTime1() {
        assertEquals("Time 1: Expected value -89",-89,data[NewLocationToPebbleDictionary.BYTE_TIME1]);
    }

    public void testTime2() {
        assertEquals("Time 2: Expected value 1",1,data[NewLocationToPebbleDictionary.BYTE_TIME2]);
    }

    public void testAltitude1() {
        assertEquals("Altitude 1: Expected value -68",-68,data[NewLocationToPebbleDictionary.BYTE_ALTITUDE1]);
    }

    public void testAltitude2() {
        assertEquals("Altitude 2: Expected value 2",2,data[NewLocationToPebbleDictionary.BYTE_ALTITUDE2]);
    }

    public void testAscent1() {
        assertEquals("Ascent 1: Expected value 38",38,data[NewLocationToPebbleDictionary.BYTE_ASCENT1]);
    }

    public void testAscent2() {
        assertEquals("Ascent 2: Expected value 0",0,data[NewLocationToPebbleDictionary.BYTE_ASCENT2]);
    }

    public void testAscentRate1() {
        assertEquals("Ascent Rate 1: Expected value 9",9,data[NewLocationToPebbleDictionary.BYTE_ASCENTRATE1]);
    }

    public void testAscentRate2() {
        assertEquals("Ascent Rate 2: Expected value 0",0,data[NewLocationToPebbleDictionary.BYTE_ASCENTRATE2]);
    }

    public void testSlope() {
        assertEquals("Slope: Expected value 13",13,data[NewLocationToPebbleDictionary.BYTE_SLOPE]);
    }

    public void testXpos1() {
        assertEquals("Xpos 1: Expected value 3",3,data[NewLocationToPebbleDictionary.BYTE_XPOS1]);
    }

    public void testXpos2() {
        assertEquals("Xpos 2: Expected value 0",0,data[NewLocationToPebbleDictionary.BYTE_XPOS2]);
    }

    public void testYPos1() {
        assertEquals("Ypos 1: Expected value 2",2,data[NewLocationToPebbleDictionary.BYTE_YPOS1]);
    }

    public void testYPos2() {
        assertEquals("Ypos 2: Expected value 0",0,data[NewLocationToPebbleDictionary.BYTE_YPOS2]);
    }

    public void testSpeed1() {
        assertEquals("Speed 1: Expected value -112",-112,data[NewLocationToPebbleDictionary.BYTE_SPEED1]);
    }

    public void testSpeed2() {
        assertEquals("Speed 2: Expected value 0",0,data[NewLocationToPebbleDictionary.BYTE_SPEED2]);
    }

    public void testBearing() {
        assertEquals("Bearing: Expected value -121",-121,data[NewLocationToPebbleDictionary.BYTE_BEARING]);
    }

    public void testHeartrate() {
        assertEquals("Heartrate: Expected value 123",123,data[NewLocationToPebbleDictionary.BYTE_HEARTRATE]);
    }
    public void testCyclingCadence() {
        assertEquals("Cycling Cadence: Expected value 134",(byte) 134,data[NewLocationToPebbleDictionary.BYTE_CADENCE]);
    }

    private boolean bitIsSet(byte b, int position)
    {
        NewLocation event = new NewLocation();
        event.setUnits(1);
        int bit = (b >> position & 1);
        return bit == 1;
    }

}
