package com.njackson.test.gps;

import android.content.SharedPreferences;
import android.location.LocationManager;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.gps.ServiceNmeaListener;

import java.util.Date;

import fr.jayps.android.AdvancedLocation;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by njackson on 24/12/14.
 */
public class ServiceNmeaListenerTest extends AndroidTestCase {

    private AdvancedLocation _mockAdvancedLocation;
    private LocationManager _mockLocationManager;
    private ServiceNmeaListener _listener;
    private SharedPreferences _preferences;
    private SharedPreferences.Editor _editor;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());

        _mockAdvancedLocation = mock(AdvancedLocation.class);
        _mockLocationManager = mock(LocationManager.class);
        _preferences = mock(SharedPreferences.class);
        _editor = mock(SharedPreferences.Editor.class);

        when(_preferences.edit()).thenReturn(_editor);

        _listener = new ServiceNmeaListener(_mockAdvancedLocation, _mockLocationManager, _preferences);
    }

    @SmallTest
    public void testOnNmeaReceivedWithValidStringSetsGeoidOnAdvancedLocation(){
        _listener.onNmeaReceived(new Date().getTime(),"$GPGGA,1,2,3,4,5,6,7,8,9,10,11,12");

        verify(_mockAdvancedLocation,times(1)).setGeoidHeight(11);
    }

    @SmallTest
    public void testOnNmeaReceivedWithInValidDoesNothing(){
        _listener.onNmeaReceived(new Date().getTime(),"bad string");

        verify(_mockAdvancedLocation,times(0)).setGeoidHeight(anyDouble());
    }

    @SmallTest
    public void testOnNmeaReceivedUnregistersListener(){
        _listener.onNmeaReceived(new Date().getTime(),"$GPGGA,1,2,3,4,5,6,7,8,9,10,11,12");

        verify(_mockLocationManager,times(1)).removeNmeaListener(_listener);
    }

    @SmallTest
    public void testOnNmeaReceivedSetsSharedPrefrences(){
        _listener.onNmeaReceived(new Date().getTime(),"$GPGGA,1,2,3,4,5,6,7,8,9,10,11,12");

        verify(_editor,times(1)).putFloat("GEOID_HEIGHT", 11.0f);
        verify(_editor,times(1)).commit();
    }

}
