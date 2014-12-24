package com.njackson.test.gps;

import android.location.LocationManager;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.gps.ServiceNmeaListener;

import java.util.Date;

import fr.jayps.android.AdvancedLocation;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by njackson on 24/12/14.
 */
public class ServiceNmeaListenerTest extends AndroidTestCase {

    private AdvancedLocation _mockAdvancedLocation;
    private ServiceNmeaListener _listener;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());

        _mockAdvancedLocation = mock(AdvancedLocation.class);
        _listener = new ServiceNmeaListener(_mockAdvancedLocation);
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

}
