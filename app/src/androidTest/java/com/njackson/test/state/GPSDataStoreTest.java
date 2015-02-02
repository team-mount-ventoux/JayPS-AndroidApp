package com.njackson.test.state;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.test.AndroidTestCase;

import com.njackson.Constants;
import com.njackson.state.GPSDataStore;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * Created by njackson on 30/01/15.
 */
public class GPSDataStoreTest extends AndroidTestCase {

    SharedPreferences _mockPreferences;
    SharedPreferences.Editor _mockEditor;
    private Resources _mockResource;
    private Context _mockContext;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());

        _mockPreferences = mock(SharedPreferences.class);
        _mockEditor = mock(SharedPreferences.Editor.class);
        _mockResource = mock(Resources.class);
        _mockContext = mock(Context.class);

        when(_mockPreferences.edit()).thenReturn(_mockEditor);
        when(_mockContext.getResources()).thenReturn(_mockResource);

        when(_mockPreferences.getString("UNITS_OF_MEASURE", "" + Constants.METRIC)).thenReturn("11");
        when(_mockPreferences.getLong("GPS_LAST_START", 0)).thenReturn(12l);
        when(_mockPreferences.getFloat("GPS_DISTANCE",0)).thenReturn(13f);
        when(_mockPreferences.getLong("GPS_ELAPSEDTIME",0)).thenReturn(14l);
        when(_mockPreferences.getFloat("GPS_ASCENT",0)).thenReturn(15f);
        when(_mockPreferences.getFloat("GEOID_HEIGHT",0)).thenReturn(16f);
        when(_mockPreferences.getFloat("GPS_FIRST_LOCATION_LAT",0)).thenReturn(17f);
        when(_mockPreferences.getFloat("GPS_FIRST_LOCATION_LON",0)).thenReturn(18f);
    }

    public void testLoadsUnitsFromPreferencesOnStart() {
        GPSDataStore store = new GPSDataStore(_mockPreferences, _mockContext);

        assertEquals(11, store.getMeasurementUnits());
    }

    public void testLoadsStartTimeFromPreferencesOnStart() {
        GPSDataStore store = new GPSDataStore(_mockPreferences, _mockContext);

        assertEquals(12l, store.getStartTime());
    }

    public void testLoadsDistanceFromPreferencesOnStart() {
        GPSDataStore store = new GPSDataStore(_mockPreferences, _mockContext);

        assertEquals(13f, store.getDistance());
    }

    public void testLoadsElapsedTimeFromPreferencesOnStart() {
        GPSDataStore store = new GPSDataStore(_mockPreferences, _mockContext);

        assertEquals(14l, store.getElapsedTime());
    }

    public void testLoadsAscentFromPreferencesOnStart() {
        GPSDataStore store = new GPSDataStore(_mockPreferences, _mockContext);

        assertEquals(15f, store.getAscent());
    }

    public void testLoadsGEOIDFromPreferencesOnStart() {
        GPSDataStore store = new GPSDataStore(_mockPreferences, _mockContext);

        assertEquals(16f, store.getGEOIDHeight());
    }

    public void testLoadsLattitudeFromPreferencesOnStart() {
        GPSDataStore store = new GPSDataStore(_mockPreferences, _mockContext);

        assertEquals(17f, store.getFirstLocationLattitude());
    }

    public void testLoadsLongitudeFromPreferencesOnStart() {
        GPSDataStore store = new GPSDataStore(_mockPreferences, _mockContext);

        assertEquals(18f, store.getFirstLocationLongitude());
    }

    public void testResetsDataOnReset() {
        GPSDataStore store = new GPSDataStore(_mockPreferences, _mockContext);

        store.resetAllValues();

        //assertEquals(0l, store.getStartTime());
        assertEquals(0f, store.getDistance());
        assertEquals(0l, store.getElapsedTime());
        assertEquals(0f, store.getAscent());
        //assertEquals(0f, store.getGEOIDHeight());
        //assertEquals(0f, store.getFirstLocationLattitude());
        //assertEquals(0f, store.getFirstLocationLongitude());
    }

    public void testSavesStartTimeToPreferencesOnCommit() {
        GPSDataStore store = new GPSDataStore(_mockPreferences, _mockContext);
        store.setStartTime(22l);
        store.commit();

        verify(_mockEditor,times(1)).putLong("GPS_LAST_START",22l);
    }

    public void testSavesDistanceToPreferencesOnCommit() {
        GPSDataStore store = new GPSDataStore(_mockPreferences, _mockContext);
        store.setDistance(23f);
        store.commit();

        verify(_mockEditor,times(1)).putFloat("GPS_DISTANCE", 23f);
    }

    public void testSavesElapsedTimeToPreferencesOnCommit() {
        GPSDataStore store = new GPSDataStore(_mockPreferences, _mockContext);
        store.setElapsedTime(24l);
        store.commit();

        verify(_mockEditor,times(1)).putLong("GPS_ELAPSEDTIME", 24l);
    }

    public void testSavesAscentToPreferencesOnCommit() {
        GPSDataStore store = new GPSDataStore(_mockPreferences, _mockContext);
        store.setAscent(25f);
        store.commit();

        verify(_mockEditor,times(1)).putFloat("GPS_ASCENT", 25f);
    }

    public void testSavesGeoidToPreferencesOnCommit() {
        GPSDataStore store = new GPSDataStore(_mockPreferences, _mockContext);
        store.setGEOIDHeight(26f);
        store.commit();

        verify(_mockEditor,times(1)).putFloat("GEOID_HEIGHT",26f);
    }

    public void testSavesLattitudeToPreferencesOnCommit() {
        GPSDataStore store = new GPSDataStore(_mockPreferences, _mockContext);
        store.setFirstLocationLattitude(27f);
        store.commit();

        verify(_mockEditor,times(1)).putFloat("GPS_FIRST_LOCATION_LAT",27f);
    }

    public void testSavesLongitudeToPreferencesOnCommit() {
        GPSDataStore store = new GPSDataStore(_mockPreferences, _mockContext);
        store.setFirstLocationLongitude(28f);
        store.commit();

        verify(_mockEditor,times(1)).putFloat("GPS_FIRST_LOCATION_LON",28f);
    }

    public void testSavesUnitsToPreferencesOnCommit() {
        GPSDataStore store = new GPSDataStore(_mockPreferences, _mockContext);
        store.setMeasurementUnits(29);
        store.commit();

        verify(_mockEditor,times(1)).putString("UNITS_OF_MEASURE", "29");
    }

    public void testCallsEditorCommitOnCommit() {
        GPSDataStore store = new GPSDataStore(_mockPreferences, _mockContext);
        store.commit();

        verify(_mockEditor,times(1)).commit();
    }
}
