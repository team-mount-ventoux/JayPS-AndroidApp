package com.njackson.state;

/**
 * Created by njackson on 30/01/15.
 */
public interface IGPSDataStore {

    int getMeasurementUnits();
    void setMeasurementUnits(int value);

    long getStartTime();
    void setStartTime(long value);

    float getDistance();
    void setDistance(float value);

    long getElapsedTime();
    void setElapsedTime(long value);

    float getAscent();
    void setAscent(float value);

    float getMaxSpeed();
    void setMaxSpeed(float value);


    float getGEOIDHeight();
    void setGEOIDHeight(float value);

    float getFirstLocationLattitude();
    void setFirstLocationLattitude(float value);

    float getFirstLocationLongitude();
    void setFirstLocationLongitude(float value);

    /* Resets all values to 0 */
    void resetAllValues();

    /* Commits changes and saves */
    void commit();

}
