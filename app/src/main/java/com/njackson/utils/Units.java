package com.njackson.utils;

import com.njackson.Constants;

/**
 * Created by jay on 12/02/15.
 */
public class Units {

    private static final String TAG = "PB-Units";

    public static boolean isPace(int units) {
        switch(units) {
            case Constants.RUNNING_IMPERIAL:
            case Constants.RUNNING_METRIC:
                return true;
        }
        return false;
    }

    public static String getSpeedUnits(int units) {
        switch(units) {
            case Constants.IMPERIAL:
                return "mph";
            case  Constants.METRIC:
                return "km/h";
            case Constants.NAUTICAL_IMPERIAL:
                return "kn";
            case Constants.NAUTICAL_METRIC:
                return "kn";
            case Constants.RUNNING_IMPERIAL:
                return "min/m";
            case Constants.RUNNING_METRIC:
                return "min/km";
        }
        return "";
    }

    public static String getDistanceUnits(int units) {
        switch(units) {
            case Constants.IMPERIAL:
                return "miles";
            case  Constants.METRIC:
                return "km";
            case Constants.NAUTICAL_IMPERIAL:
                return "nm";
            case Constants.NAUTICAL_METRIC:
                return "nm";
            case Constants.RUNNING_IMPERIAL:
                return "miles";
            case Constants.RUNNING_METRIC:
                return "km";
        }
        return "";
    }

    public static String getAltitudeUnits(int units) {
        switch(units) {
            case Constants.IMPERIAL:
                return "ft";
            case  Constants.METRIC:
                return "m";
            case Constants.NAUTICAL_IMPERIAL:
                return "ft";
            case Constants.NAUTICAL_METRIC:
                return "m";
            case Constants.RUNNING_IMPERIAL:
                return "ft";
            case Constants.RUNNING_METRIC:
                return "m";
        }
        return "";
    }

    public static String getAscentRateUnits(int units) {
        switch(units) {
            case Constants.IMPERIAL:
                return "ft/h";
            case  Constants.METRIC:
                return "m/h";
            case Constants.NAUTICAL_IMPERIAL:
                return "ft/h";
            case Constants.NAUTICAL_METRIC:
                return "m/h";
            case Constants.RUNNING_IMPERIAL:
                return "ft/h";
            case Constants.RUNNING_METRIC:
                return "m/h";
        }
        return "";
    }
    public static String getTemperatureUnits(int units) {
        switch(units) {
            case Constants.IMPERIAL:
            case Constants.NAUTICAL_IMPERIAL:
            case Constants.RUNNING_IMPERIAL:
                return "°F";
            case  Constants.METRIC:
            case Constants.NAUTICAL_METRIC:
            case Constants.RUNNING_METRIC:
                return "°C";
        }
        return "";
    }
}
