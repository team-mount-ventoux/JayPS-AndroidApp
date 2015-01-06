package com.njackson.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created by njackson on 25/12/14.
 */
public class NumberConverter {

    private static final String TAG = "PB-NumberConverter";

    public String converFloatToString(float number, int decimalPlaces) {
        try {
            BigDecimal bd = new BigDecimal(number).setScale(decimalPlaces, RoundingMode.HALF_EVEN);
            return bd.toPlainString();
        } catch (NumberFormatException e) {
            return "."; // NaN or Infinity, only display "void" number
        }
    }
}
