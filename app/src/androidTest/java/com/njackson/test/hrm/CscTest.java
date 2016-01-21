package com.njackson.test.hrm;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.sensor.Csc;

public class CscTest extends AndroidTestCase {

    @SmallTest
    public void testCscRPM() throws Exception {
        Csc csc = new Csc();

        csc.onNewValues(0, 1, 0, 1);
        csc.onNewValues(2, 1025, 1, 1025);
        assertEquals(120f, csc.getWheelRpm());
        assertEquals(60f, csc.getCrankRpm());

        csc.onNewValues(16904, 37896, 6473, 37192);

        csc.onNewValues(16905, 38854, 6474, 38349);
        assertEquals(64.13361f, csc.getWheelRpm());
        assertEquals(53.10285f, csc.getCrankRpm());

        csc.onNewValues(16905, 38854, 6474, 38349);
        assertEquals(64.13361f, csc.getWheelRpm());
        assertEquals(53.10285f, csc.getCrankRpm());

        csc.onNewValues(16906, 39812, 6475, 39462);
        assertEquals(64.133611f, csc.getWheelRpm());
        assertEquals(55.202156f, csc.getCrankRpm());
    }

}
