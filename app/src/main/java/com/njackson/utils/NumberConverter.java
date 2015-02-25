package com.njackson.utils;

import android.text.format.DateUtils;
import android.util.Log;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created by njackson on 25/12/14.
 */
public class NumberConverter {

    private static final String TAG = "PB-NumberConverter";

    public String convertFloatToString(float number, int decimalPlaces) {
        try {
            BigDecimal bd = new BigDecimal(number).setScale(decimalPlaces, RoundingMode.HALF_EVEN);
            return bd.toPlainString();
        } catch (NumberFormatException e) {
            return "."; // NaN or Infinity, only display "void" number
        }
    }

    public String convertSpeedToPace(float number) {
        return DateUtils.formatElapsedTime((int) (60 * number));
    }
}
