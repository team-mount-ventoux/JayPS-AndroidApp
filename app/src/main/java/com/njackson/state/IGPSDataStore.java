package com.njackson.state;

/**
 * Created by njackson on 30/01/15.
 */
public interface IGPSDataStore {

    // reload data modified in PreferenceActivity
    public void reloadPreferencesFromSettings();

    int getMeasurementUnits();
    void setMeasurementUnits(int value);

    long getStartTime();
    long getPrevStartTime();
    void setStartTime(long value);

    float getDistance();
    void setDistance(float value);

    long getElapsedTime();
    void setElapsedTime(long value);

    float getAscent();
    void setAscent(float value);

    int getNbAscent();
    void setNbAscent(int value);

    float getMaxSpeed();
    void setMaxSpeed(float value);

    float getAltitudeCalibrationDelta(long time);
    void setAltitudeCalibrationDelta(float value, long time);

    float getGEOIDHeight();
    void setGEOIDHeight(float value);

    float getFirstLocationLattitude();
    void setFirstLocationLattitude(float value);

    float getFirstLocationLongitude();
    void setFirstLocationLongitude(float value);

    float getLastLocationLatitude();
    void setLastLocationLatitude(float value);

    float getLastLocationLongitude();
    void setLastLocationLongitude(float value);

    /* Resets all values to 0 */
    void resetAllValues();

    /* Commits changes and saves */
    void commit();

}
