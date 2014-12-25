package com.njackson.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created by njackson on 25/12/14.
 */
public class NumberConverter {
    public String converFloatToString(float number, int decimalPlaces) {
        BigDecimal bd = new BigDecimal(number).setScale(decimalPlaces, RoundingMode.HALF_EVEN);
        return bd.toPlainString();
    }
}
