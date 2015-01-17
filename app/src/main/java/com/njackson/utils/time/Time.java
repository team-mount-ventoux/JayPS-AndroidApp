package com.njackson.utils.time;

import java.util.Date;

/**
 * Created by njackson on 17/01/15.
 */
public class Time implements ITime {
    @Override
    public Date getCurrentDate() {
        return new Date();
    }

    @Override
    public long getCurrentTimeMilliseconds() {
        return new Date().getTime();
    }
}
