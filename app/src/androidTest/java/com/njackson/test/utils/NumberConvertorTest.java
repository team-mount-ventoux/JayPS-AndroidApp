package com.njackson.test.utils;

import android.test.AndroidTestCase;

import com.njackson.utils.NumberConverter;

/**
 * Created by njackson on 25/12/14.
 */
public class NumberConvertorTest extends AndroidTestCase {

    private NumberConverter _numberConverter;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        _numberConverter = new NumberConverter();
    }

    public void testConvertsStringWith2DPSuccesfully() {
        String convertedValue = _numberConverter.convertFloatToString(23.345343f,2);

        assertEquals(convertedValue,"23.35");
    }

    public void testConvertsStringWith0DPSuccesfully() {
        String convertedValue = _numberConverter.convertFloatToString(23.345343f,0);

        assertEquals(convertedValue,"23");
    }

    public void testReturnsNaNWhenNotANumber() {
        String convertedValue = _numberConverter.convertFloatToString(Float.NaN,0);

        assertEquals(convertedValue,".");
    }

}
