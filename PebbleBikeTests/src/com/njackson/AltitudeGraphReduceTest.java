package com.njackson;

import android.test.AndroidTestCase;
import com.njackson.util.AltitudeGraphReduce;
import junit.framework.Assert;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class com.njackson.MainActivityTest \
 * com.njackson.tests/android.test.InstrumentationTestRunner
 */
public class AltitudeGraphReduceTest extends AndroidTestCase {

    public void  setUp() {

    }

    public void tearDown() {
        AltitudeGraphReduce.getInstance().restData();
    }

    public void testMinAltitude() {

        AltitudeGraphReduce alt = AltitudeGraphReduce.getInstance();
        int minAlt = 100;
        int maxAlt = 200;

        for(int n = minAlt; n != maxAlt + 10; n+=10) {
            alt.addAltitude(n, 0, 0);
        }

        assertTrue(
                "Min Altitude should be " + minAlt + " value: " + alt.getMin(),
                minAlt == alt.getMin());

    }

    public void testMaxAltitude() {

        AltitudeGraphReduce alt = AltitudeGraphReduce.getInstance();
        int minAlt = 100;
        int maxAlt = 200;

        for(int n = minAlt; n != maxAlt + 10; n+=10) {
            alt.addAltitude(n, 0, 0);
        }

        assertTrue(
                "Max Altitude should be " + maxAlt + " value: " + alt.getMax(),
                maxAlt == alt.getMax());

    }

    public void testCacheSize() throws InterruptedException {
        AltitudeGraphReduce alt = AltitudeGraphReduce.getInstance();
        int binInterval = 100;
        alt.setBinInterval(binInterval);

        int minAlt = 100;
        int maxAlt = 300;

        for(int n = minAlt; n != maxAlt; n+=10) {
            alt.addAltitude(n, 0, 0);
            Thread.sleep(binInterval);
        }

        int cacheSize = alt.getCache().size();
        int expectedSize = ((maxAlt - minAlt) /10);
        assertTrue(
                "Cache should contain " + expectedSize + " items contains " + cacheSize,
                 cacheSize == expectedSize);
    }

    public void testBinData() throws InterruptedException {
        AltitudeGraphReduce alt = AltitudeGraphReduce.getInstance();
        int binInterval = 100;
        alt.setBinInterval(binInterval);

        int minAlt = 100;
        int maxAlt = 3000;

        for(int n = minAlt; n != maxAlt; n+=10) {
            alt.addAltitude(n, 0, 0);
            Thread.sleep(binInterval);
        }

        int[] graphData = alt.getGraphData();
        assertTrue("Graph contains 14 bins", graphData.length == 14);

    }
}
