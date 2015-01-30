package com.njackson.test.gps;

import android.content.SharedPreferences;
import android.location.LocationManager;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.gps.ServiceNmeaListener;
import com.njackson.state.IGPSDataStore;

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
    private IGPSDataStore _dataStore;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());

        _mockAdvancedLocation = mock(AdvancedLocation.class);
        _mockLocationManager = mock(LocationManager.class);
        _dataStore = mock(IGPSDataStore.class);

        _listener = new ServiceNmeaListener(_mockAdvancedLocation, _mockLocationManager, _dataStore);
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

        verify(_dataStore,times(1)).setGEOIDHeight(11f);
        verify(_dataStore,times(1)).commit();
    }

}
