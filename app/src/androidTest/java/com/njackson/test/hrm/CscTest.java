package com.njackson.test.hrm;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.hrm.Csc;

import fr.jayps.android.AdvancedLocation;

public class CscTest extends AndroidTestCase {

    @SmallTest
    public void testCscRPM() throws Exception {
        Csc csc = new Csc();

        // first values, skipped
        csc.onNewValues(0, 0, 0, 0);

        csc.onNewValues(2, 1024, 1, 1024);
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
