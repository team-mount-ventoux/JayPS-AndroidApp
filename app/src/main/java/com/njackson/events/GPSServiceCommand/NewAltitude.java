package com.njackson.events.GPSServiceCommand;

import java.security.InvalidParameterException;

/**
 * Created by server on 11/04/2014.
 */
public class NewAltitude {

    private final int VALUES_SIZE = 14;

    private int[] _altitudeValues;

    public int[] getAltitudes() {
        return _altitudeValues;
    }

    public NewAltitude(int[] altitudeValues) {
        if(altitudeValues == null || altitudeValues.length != VALUES_SIZE)
            throw new InvalidParameterException("Constructor requires altitude values with size" + VALUES_SIZE);

        _altitudeValues = altitudeValues;
    }
}
