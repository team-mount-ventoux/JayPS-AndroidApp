package com.njackson.test.gps;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.gps.GPSSensorEventListener;
import com.njackson.gps.ServiceNmeaListener;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import fr.jayps.android.AdvancedLocation;

import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by njackson on 24/12/14.
 */
public class GPSSensorEventListenerTest extends AndroidTestCase {
    private AdvancedLocation _mockAdvancedLocation;
    private GPSSensorEventListener _listener;
    private CountDownLatch _latch;
    private SensorManager _sensorManager;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());

        _latch = new CountDownLatch(1);
        _mockAdvancedLocation = mock(AdvancedLocation.class);

        _sensorManager = mock(SensorManager.class);

        _listener = new GPSSensorEventListener(_mockAdvancedLocation,_sensorManager, new Callable() {
            @Override
            public Object call() throws Exception {
                _latch.countDown();
                return null;
            }
        });
    }

    @SmallTest
    public void testWhenOnAltitudeChangedWithPressureFiredSetsAltitudeChangeOnAdvancedLocation() {
        float[] data = new float[] {1.0f};
        _listener.sensorChanged(Sensor.TYPE_PRESSURE, data);

        verify(_mockAdvancedLocation,times(1)).onAltitudeChanged(anyFloat());
    }

    @SmallTest
    public void testWhenOnAltitudeChangedWithPressureFiredCallbackCalled() throws InterruptedException {
        float[] data = new float[] {1.0f};
        _listener.sensorChanged(Sensor.TYPE_PRESSURE, data);

        _latch.await(2000, TimeUnit.MILLISECONDS);
        assertEquals(0,_latch.getCount());
    }

    @SmallTest
    public void testWhenOnAltitudeChangedWithSomethingOtherThanPressureDoesNotSetAltitudeChangeOnAdvancedLocation() {
        float[] data = new float[] {1.0f};
        _listener.sensorChanged(Sensor.TYPE_ACCELEROMETER, data);

        verify(_mockAdvancedLocation,times(0)).onAltitudeChanged(anyFloat());
    }

    @SmallTest
    public void testWhenOnAltitudeChangedWithSoemthingOtherThanPressureDoesNotCallCallback() throws InterruptedException {
        float[] data = new float[] {1.0f};
        _listener.sensorChanged(Sensor.TYPE_ACCELEROMETER, data);

        _latch.await(2000, TimeUnit.MILLISECONDS);
        assertEquals(1,_latch.getCount());
    }

}
