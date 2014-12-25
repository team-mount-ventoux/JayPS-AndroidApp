package com.njackson.test.utils;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.utils.AltitudeGraphReduce;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

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

    private AltitudeGraphReduce _graphReduce;

    @Override
    public void setUp() {
        _graphReduce = new AltitudeGraphReduce();
    }

    public void testMinAltitude() {

        int minAlt = 100;
        int maxAlt = 200;

        for(int n = minAlt; n != maxAlt + 10; n+=10) {
            _graphReduce.addAltitude(n, 0, 0);
        }

        assertTrue(
                "Min Altitude should be " + minAlt + " value: " + _graphReduce.getMin(),
                minAlt == _graphReduce.getMin());

    }

    @SmallTest
    public void testMaxAltitude() {

        int minAlt = 100;
        int maxAlt = 200;

        for(int n = minAlt; n != maxAlt + 10; n+=10) {
            _graphReduce.addAltitude(n, 0, 0);
        }

        assertTrue(
                "Max Altitude should be " + maxAlt + " value: " + _graphReduce.getMax(),
                maxAlt == _graphReduce.getMax());

    }

    @SmallTest
    public void testCacheSize() throws InterruptedException {

        int binInterval = 100;
        _graphReduce.setBinInterval(binInterval);

        int minAlt = 100;
        int maxAlt = 300;
        int elapsedTime = 0;

        for(int n = minAlt; n != maxAlt; n+=10) {
            _graphReduce.addAltitude(n, elapsedTime, 0);
            elapsedTime += binInterval;
        }

        int cacheSize = _graphReduce.getCache().size();
        int expectedSize = ((maxAlt - minAlt) /10);
        assertTrue(
                "Cache should contain " + expectedSize + " items contains " + cacheSize,
                 cacheSize == expectedSize);
    }

    @SmallTest
    public void testBinData() throws InterruptedException {
        int binInterval = 100;
        _graphReduce.setBinInterval(binInterval);

        int minAlt = 100;
        int maxAlt = 3000;
        int elapsedTime = 0;

        for(int n = minAlt; n != maxAlt; n+=10) {
            _graphReduce.addAltitude(n, elapsedTime, 0);
            elapsedTime += 100;
        }

        int[] graphData = _graphReduce.getGraphData();
        assertTrue("Graph contains 14 bins", graphData.length == 14);
    }

    @SmallTest
    public void testResetData() {
        ArrayList<Integer> cache = new ArrayList<>(Arrays.asList(1,2,3));
        _graphReduce.setCache(cache);
        _graphReduce.setMin(1000);
        _graphReduce.setMax(2000);

        _graphReduce.resetData();

        assertEquals(_graphReduce.getMax(),0);
        assertEquals(_graphReduce.getMin(),99999);
        assertEquals(_graphReduce.getCache().size(),0);
    }
}
