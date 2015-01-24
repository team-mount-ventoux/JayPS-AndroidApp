package com.njackson.test.adapters;

import android.test.AndroidTestCase;

import com.njackson.Constants;
import com.njackson.adapters.AdvancedLocationToNewLocation;
import com.njackson.events.GPSServiceCommand.NewLocation;

import fr.jayps.android.AdvancedLocation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by njackson on 18/01/15.
 */
public class AdvancedLocationToNewLocationTest extends AndroidTestCase{

    private AdvancedLocation _mockAdvancedLocation;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());

        _mockAdvancedLocation = mock(AdvancedLocation.class);
    }

    public void testSetsDistanceCorrectlyWhenUnitsImperial() {
        when(_mockAdvancedLocation.getDistance()).thenReturn(1000f);

        NewLocation event = new AdvancedLocationToNewLocation(_mockAdvancedLocation,0,0, Constants.IMPERIAL);

        assertEquals(1000f * Constants.M_TO_MILES,event.getDistance());
    }

    public void testSetsDistanceCorrectlyWhenUnitsMetric() {
        when(_mockAdvancedLocation.getDistance()).thenReturn(1000f);

        NewLocation event = new AdvancedLocationToNewLocation(_mockAdvancedLocation,0,0, Constants.METRIC);

        assertEquals(1000f * Constants.M_TO_KM,event.getDistance());
    }

    public void testSetsSpeedCorrectlyWhenUnitsImperial() {
        when(_mockAdvancedLocation.getSpeed()).thenReturn(1000f);

        NewLocation event = new AdvancedLocationToNewLocation(_mockAdvancedLocation,0,0, Constants.IMPERIAL);

        assertEquals(1000f * Constants.MS_TO_MPH,event.getSpeed());
    }

    public void testSetsSpeedCorrectlyWhenUnitsMetric() {
        when(_mockAdvancedLocation.getSpeed()).thenReturn(1000f);

        NewLocation event = new AdvancedLocationToNewLocation(_mockAdvancedLocation,0,0, Constants.METRIC);

        assertEquals(1000f * Constants.MS_TO_KPH,event.getSpeed());
    }

    public void testSetsAverageSpeedCorrectlyWhenUnitsImperial() {
        when(_mockAdvancedLocation.getAverageSpeed()).thenReturn(1000f);

        NewLocation event = new AdvancedLocationToNewLocation(_mockAdvancedLocation,0,0, Constants.IMPERIAL);

        assertEquals(1000f * Constants.MS_TO_MPH,event.getAverageSpeed());
    }

    public void testSetsAverageSpeedCorrectlyWhenUnitsMetric() {
        when(_mockAdvancedLocation.getAverageSpeed()).thenReturn(1000f);

        NewLocation event = new AdvancedLocationToNewLocation(_mockAdvancedLocation,0,0, Constants.METRIC);

        assertEquals(1000f * Constants.MS_TO_KPH,event.getAverageSpeed());
    }

    public void testSetsLatitudeCorrectly() {
        when(_mockAdvancedLocation.getLatitude()).thenReturn(12345.0);

        NewLocation event = new AdvancedLocationToNewLocation(_mockAdvancedLocation,0,0, Constants.IMPERIAL);

        assertEquals(12345.0,event.getLatitude());
    }

    public void testSetsLongitudeCorrectly() {
        when(_mockAdvancedLocation.getLongitude()).thenReturn(123456.0);

        NewLocation event = new AdvancedLocationToNewLocation(_mockAdvancedLocation,0,0, Constants.IMPERIAL);

        assertEquals(123456.0,event.getLongitude());
    }

    public void testSetsAltitudeCorrectlyWhenUnitsImperial() {
        when(_mockAdvancedLocation.getAltitude()).thenReturn(1000.0);

        NewLocation event = new AdvancedLocationToNewLocation(_mockAdvancedLocation,0,0, Constants.IMPERIAL);

        assertEquals(1000.0 * Constants.M_TO_FEET,event.getAltitude());
    }

    public void testSetsAltitudeCorrectlyWhenUnitsMetric() {
        when(_mockAdvancedLocation.getAltitude()).thenReturn(1000.0);

        NewLocation event = new AdvancedLocationToNewLocation(_mockAdvancedLocation,0,0, Constants.METRIC);

        assertEquals(1000.0 * Constants.M_TO_M,event.getAltitude());
    }

    public void testSetsAscentCorrectlyWhenUnitsImperial() {
        when(_mockAdvancedLocation.getAscent()).thenReturn(1000.0);

        NewLocation event = new AdvancedLocationToNewLocation(_mockAdvancedLocation,0,0, Constants.IMPERIAL);

        assertEquals(1000.0 * Constants.M_TO_FEET,event.getAscent());
    }

    public void testSetsAscentCorrectlyWhenUnitsMetric() {
        when(_mockAdvancedLocation.getAscent()).thenReturn(1000.0);

        NewLocation event = new AdvancedLocationToNewLocation(_mockAdvancedLocation,0,0, Constants.METRIC);

        assertEquals(1000.0 * Constants.M_TO_M,event.getAscent());
    }

    public void testSetsAscentRateCorrectlyWhenUnitsImperial() {
        when(_mockAdvancedLocation.getAscentRate()).thenReturn(1000f);

        NewLocation event = new AdvancedLocationToNewLocation(_mockAdvancedLocation,0,0, Constants.IMPERIAL);

        assertEquals(3600f * 1000f * Constants.M_TO_FEET,event.getAscentRate());
    }

    public void testSetsAscentRateCorrectlyWhenUnitsMetric() {
        when(_mockAdvancedLocation.getAscentRate()).thenReturn(1000f);

        NewLocation event = new AdvancedLocationToNewLocation(_mockAdvancedLocation,0,0, Constants.METRIC);

        assertEquals(3600f * 1000f * Constants.M_TO_M,event.getAscentRate());
    }

    public void testSetsSlopeCorrectlyWhenUnitsMetric() {
        when(_mockAdvancedLocation.getSlope()).thenReturn(1000f);

        NewLocation event = new AdvancedLocationToNewLocation(_mockAdvancedLocation,0,0, Constants.METRIC);

        assertEquals(100f * 1000f,event.getSlope());
    }

    public void testSetsAccuracyCorrectlyWhenUnitsMetric() {
        when(_mockAdvancedLocation.getAccuracy()).thenReturn(1000f);

        NewLocation event = new AdvancedLocationToNewLocation(_mockAdvancedLocation,0,0, Constants.METRIC);

        assertEquals(1000f,event.getAccuracy());
    }

    public void testSetsTimeCorrectlyWhenUnitsMetric() {
        when(_mockAdvancedLocation.getTime()).thenReturn(1000l);

        NewLocation event = new AdvancedLocationToNewLocation(_mockAdvancedLocation,0,0, Constants.METRIC);

        assertEquals(1000l,event.getTime());
    }

    public void testSetsElapsedTimeCorrectlyWhenUnitsMetric() {
        when(_mockAdvancedLocation.getElapsedTime()).thenReturn(1000l);

        NewLocation event = new AdvancedLocationToNewLocation(_mockAdvancedLocation,0,0, Constants.METRIC);

        assertEquals(1,event.getElapsedTimeSeconds());
    }

    public void testSetsXPosCorrectlyWhenUnitsMetric() {
        NewLocation event = new AdvancedLocationToNewLocation(_mockAdvancedLocation,1,2, Constants.METRIC);

        assertEquals(1.0,event.getXpos());
    }

    public void testSetsYPosCorrectlyWhenUnitsMetric() {
        NewLocation event = new AdvancedLocationToNewLocation(_mockAdvancedLocation,1,2, Constants.METRIC);

        assertEquals(2.0,event.getYpos());
    }

    public void testSetsBearingCorrectlyWhenUnitsMetric() {
        when(_mockAdvancedLocation.getBearing()).thenReturn(1000f);

        NewLocation event = new AdvancedLocationToNewLocation(_mockAdvancedLocation,0,0, Constants.METRIC);

        assertEquals(1000d,event.getBearing());
    }

    public void testSetsHearRateCorrectlyWhenUnitsMetric() {
        NewLocation event = new AdvancedLocationToNewLocation(_mockAdvancedLocation,0,0, Constants.METRIC);

        assertEquals(255,event.getHeartRate());
    }
}
