package com.njackson.test.utils.adapters;

import com.getpebble.android.kit.util.PebbleDictionary;
import com.njackson.Constants;
import com.njackson.events.GPSService.NewLocation;
import com.njackson.utils.adapters.LocationEventToPebbleData;

import junit.framework.TestCase;

/**
 * Created by server on 25/03/2014.
 */
public class LocationEventToPebbleDataTest extends TestCase{

    public void testConvertsNewLocationEventToDictionary() {

        NewLocation event = new NewLocation();
        event.setUnits(1);
        event.setBearing(190.1);
        event.setYpos(2.2);
        event.setXpos(3.3);
        event.setElapsedTimeSeconds(423223);
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

        PebbleDictionary dic = LocationEventToPebbleData.convert(event, true, true, true, 5000, 123);
        byte[] data = dic.getBytes(Constants.PEBBLE_LOCTATION_DATA);

        assertNotNull("Data should not be null",data);
        assertEquals("Expected units bit to be true",true,bitIsSet(data[0],0));
        assertEquals("Expected service running bit to be true",true,bitIsSet(data[0], 1));
        assertEquals("Expected debug bit to be true",true,bitIsSet(data[0],2));
        assertEquals("Expected live tracking bit to be true",true,bitIsSet(data[0],3));
        assertEquals("Expected refresh of 3",3,data[0] >> 4);

        assertEquals("Accuracy: Expected value 7",7,data[LocationEventToPebbleData.BYTE_ACCURACY]);
        assertEquals("Distance 1: Expected value 1",1,data[LocationEventToPebbleData.BYTE_DISTANCE1]);
        assertEquals("Distance 2: Expected value 0",0,data[LocationEventToPebbleData.BYTE_DISTANCE2]);
        assertEquals("Time 1: Expected value -89",-89,data[LocationEventToPebbleData.BYTE_TIME1]);
        assertEquals("Time 2: Expected value 1",1,data[LocationEventToPebbleData.BYTE_TIME2]);
        assertEquals("Altitude 1: Expected value -68",-68,data[LocationEventToPebbleData.BYTE_ALTITUDE1]);
        assertEquals("Altitude 2: Expected value 2",2,data[LocationEventToPebbleData.BYTE_ALTITUDE2]);
        assertEquals("Ascent 1: Expected value 38",38,data[LocationEventToPebbleData.BYTE_ASCENT1]);
        assertEquals("Ascent 2: Expected value 0",0,data[LocationEventToPebbleData.BYTE_ASCENT2]);
        assertEquals("Ascent Rate 1: Expected value 9",9,data[LocationEventToPebbleData.BYTE_ASCENTRATE1]);
        assertEquals("Ascent Rate 2: Expected value 0",0,data[LocationEventToPebbleData.BYTE_ASCENTRATE2]);
        assertEquals("Slope: Expected value 13",13,data[LocationEventToPebbleData.BYTE_SLOPE]);
        assertEquals("Xpos 1: Expected value 3",3,data[LocationEventToPebbleData.BYTE_XPOS1]);
        assertEquals("Xpos 2: Expected value 0",0,data[LocationEventToPebbleData.BYTE_XPOS2]);
        assertEquals("Ypos 1: Expected value 2",2,data[LocationEventToPebbleData.BYTE_YPOS1]);
        assertEquals("Ypos 2: Expected value 0",0,data[LocationEventToPebbleData.BYTE_YPOS2]);
        assertEquals("Speed 1: Expected value 6",6,data[LocationEventToPebbleData.BYTE_SPEED1]);
        assertEquals("Speed 2: Expected value 2",2,data[LocationEventToPebbleData.BYTE_SPEED2]);
        assertEquals("Bearing: Expected value -121",-121,data[LocationEventToPebbleData.BYTE_BEARING]);
        assertEquals("Heartrate: Expected value 123",123,data[LocationEventToPebbleData.BYTE_HEARTRATE]);

    }

    public void testDistanceConversionMetric() {
        NewLocation event = new NewLocation();
        event.setUnits(1);
        event.setDistance(124.4f);
        PebbleDictionary dic = LocationEventToPebbleData.convert(event, true, true, true, 5000, 123);
        byte[] data = dic.getBytes(Constants.PEBBLE_LOCTATION_DATA);

        assertEquals("Distance 1: Expected value 12",12,data[LocationEventToPebbleData.BYTE_DISTANCE1]);
        assertEquals("Distance 2: Expected value 0",0,data[LocationEventToPebbleData.BYTE_DISTANCE2]);
    }

    public void testDistanceConversionImperial() {
        NewLocation event = new NewLocation();
        event.setUnits(0);
        event.setDistance(124.4f);
        PebbleDictionary dic = LocationEventToPebbleData.convert(event, true, true, true, 5000, 123);
        byte[] data = dic.getBytes(Constants.PEBBLE_LOCTATION_DATA);

        assertEquals("Distance 1: Expected value 7",7,data[LocationEventToPebbleData.BYTE_DISTANCE1]);
        assertEquals("Distance 2: Expected value 0",0,data[LocationEventToPebbleData.BYTE_DISTANCE2]);
    }

    public void testSpeedConversionMetric() {
        NewLocation event = new NewLocation();
        event.setUnits(1);
        event.setSpeed(14.4f);
        PebbleDictionary dic = LocationEventToPebbleData.convert(event, true, true, true, 5000, 123);
        byte[] data = dic.getBytes(Constants.PEBBLE_LOCTATION_DATA);

        assertEquals("Speed 1: Expected value 6",6,data[LocationEventToPebbleData.BYTE_SPEED1]);
        assertEquals("Speed 2: Expected value 2",2,data[LocationEventToPebbleData.BYTE_SPEED2]);
    }

    public void testSpeedConversionImperial() {
        NewLocation event = new NewLocation();
        event.setUnits(0);
        event.setSpeed(14.4f);
        PebbleDictionary dic = LocationEventToPebbleData.convert(event, true, true, true, 5000, 123);
        byte[] data = dic.getBytes(Constants.PEBBLE_LOCTATION_DATA);

        assertEquals("Speed 1: Expected value 66",66,data[LocationEventToPebbleData.BYTE_SPEED1]);
        assertEquals("Speed 2: Expected value 0",1,data[LocationEventToPebbleData.BYTE_SPEED2]);
    }

    public void testAltitudeConversionMetric() {
        NewLocation event = new NewLocation();
        event.setUnits(1);
        event.setAltitude(4000);
        PebbleDictionary dic = LocationEventToPebbleData.convert(event, true, true, true, 5000, 123);
        byte[] data = dic.getBytes(Constants.PEBBLE_LOCTATION_DATA);

        assertEquals("Altitude 1: Expected value -96",-96,data[LocationEventToPebbleData.BYTE_ALTITUDE1]);
        assertEquals("Altitude 2: Expected value 15",15,data[LocationEventToPebbleData.BYTE_ALTITUDE2]);
    }

    private boolean bitIsSet(byte b, int position)
    {
        NewLocation event = new NewLocation();
        event.setUnits(1);
        int bit = (b >> position & 1);
        return bit == 1;
    }

    public void testAltitudeConversionImperial() {
        NewLocation event = new NewLocation();
        event.setUnits(0);
        event.setAltitude(4000);
        PebbleDictionary dic = LocationEventToPebbleData.convert(event, true, true, true, 5000, 123);
        byte[] data = dic.getBytes(Constants.PEBBLE_LOCTATION_DATA);

        assertEquals("Altitude 1: Expected value 67",67,data[LocationEventToPebbleData.BYTE_ALTITUDE1]);
        assertEquals("Altitude 2: Expected value 51",51,data[LocationEventToPebbleData.BYTE_ALTITUDE2]);
    }

}
