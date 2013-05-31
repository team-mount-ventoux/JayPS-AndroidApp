package fr.jayps.android;

import android.location.Location;
import android.util.Log;
import android.content.Context;
import android.widget.Toast;

public class MyLocation {
    private static final String TAG = "MyLocation";

    public Location lastLocation = null;            // last received location
    public Location lastGoodLocation = null;        // last location with accuracy below _minAccuracy
    public Location lastGoodAscentLocation = null;  // last location with changed ascent
    public Location firstLocation = null;           // first received location
    public Location firstGoodLocation = null;       // first location with accuracy below _minAccuracy
    
    protected float _minAccuracy = 10;   // in m
    protected float _minAccuracyIni = _minAccuracy;
    
    // always remember that accuracy is 3x worth on altitude than on latitude/longitude
    protected float _minAccuracyForAltitudeChangeLevel1 = 4; // in m
    protected float _minAltitudeChangeLevel1 = 10; // in m
    protected float _minAccuracyForAltitudeChangeLevel2 = 8; // in m
    protected float _minAltitudeChangeLevel2 = 20; // in m
    protected float _minAccuracyForAltitudeChangeLevel3 = 15; // in m
    protected float _minAltitudeChangeLevel3 = 50; // in m

    protected long _minDeltaTimeToSaveLocation = 3000; // in ms
    protected float _minDeltaDistanceToSaveLocation = 20;   // in m
    
    // min speed to compute _elapsedTime or _ascent
    // 0.3m/s <=> 1.08km/h
    protected float _minSpeedToComputeStats = 0.3f; // in m/s 
    
    public int nbOnLocationChanged = 0;
    public int nbGoodLocations = 0;
    protected int _nbBadAccuracyLocations = 0;
    
    protected float _distance = 0; // in m
    protected double _ascent = 0; // in m
    protected long _elapsedTime = 0; // in ms

    float _averageSpeed = 0; // in m/s

    

    protected Context _context = null;
    public MyLocation() {
        this._context = null;
    }    
    public MyLocation(Context context) {
        this._context = context;
    }

    
    public double getAltitude() {
        if (lastGoodLocation != null) {
            return lastGoodLocation.getAltitude();
        }
        return 0;
    }
    public double getGoodAltitude() {
        if (lastGoodAscentLocation != null) {
            return lastGoodAscentLocation.getAltitude();
        }
        return 0;
    }

    public float getAccuracy() {
        if (lastLocation != null) {
            return lastLocation.getAccuracy();
        }
        return 0.0f;
    }    
    public float getSpeed() {
        if (lastLocation != null) {
            return lastLocation.getSpeed();
        }
        return 0.0f;
    }
    public float getAverageSpeed() {
        return _averageSpeed;
    }
    public long getElapsedTime() {
        return _elapsedTime;
    }
    public float getDistance() {
        return _distance;
    }    
    public double getAscent() {
        return _ascent;
    }    

    public void onLocationChanged(Location location) {
        long deltaTime = 0;
        float deltaDistance = 0;
        double deltaAscent = 0;

        nbOnLocationChanged++;
        Logger("onLocationChanged: " +nbGoodLocations+"/"+nbOnLocationChanged+" Alt:"+ location.getAltitude() + "m-" + location.getAccuracy() + "m " + location.getLatitude() + "-" + location.getLongitude());
        
        if (firstLocation == null) {
            // 1st location
            firstLocation = lastLocation = location;
        }

        deltaTime = location.getTime() - lastLocation.getTime();
    
        if ((lastGoodLocation != null) && ((location.getTime() - lastGoodLocation.getTime()) < 1000)) {
            // less than 1000ms, skip this location
            return;
        }
        
        deltaDistance = location.distanceTo(lastLocation);

        if (location.getAccuracy() > _minAccuracy) {
            _nbBadAccuracyLocations++;
            if (_nbBadAccuracyLocations > 10) {
                
                _minAccuracy = (float) Math.floor(1.5f * _minAccuracy);
                _nbBadAccuracyLocations = 0;
                
                Logger("accuracy to often above _minAccuracy, augment _minAccuracy to " + _minAccuracy,  LoggerType.TOAST);
            }
        }

        if (location.getAccuracy() <= _minAccuracy) {

            if (firstGoodLocation == null) {
                firstGoodLocation = location;
            }  
            
            if ((location.getAccuracy() <= _minAccuracyIni) && (_minAccuracy > _minAccuracyIni)) {
                _minAccuracy = _minAccuracyIni;

                Logger("_minAccuracy returns to initial value: " + _minAccuracy, LoggerType.TOAST);
            }
        
            float localAverageSpeed = (float) deltaDistance / ((float) deltaTime / 1000f); // in m/s
            
            //Logger("localAverageSpeed:" + localAverageSpeed + " speed=" + location.getSpeed());
            
            // additional conditions to compute statistics
            if (
                  (_distance == 0) // 1st location
                ||
                  (localAverageSpeed > _minSpeedToComputeStats)
            ) {

                if (lastGoodAscentLocation == null) {
                    lastGoodAscentLocation = location;
                }

                if (lastGoodAscentLocation != null && (
                    ((Math.abs(location.getAltitude() - lastGoodAscentLocation.getAltitude()) >= _minAltitudeChangeLevel1) && (location.getAccuracy() <= _minAccuracyForAltitudeChangeLevel1))
                        ||
                    ((Math.abs(location.getAltitude() - lastGoodAscentLocation.getAltitude()) >= _minAltitudeChangeLevel2) && (location.getAccuracy() <= _minAccuracyForAltitudeChangeLevel2))
                        ||
                    ((Math.abs(location.getAltitude() - lastGoodAscentLocation.getAltitude()) >= _minAltitudeChangeLevel3) && (location.getAccuracy() <= _minAccuracyForAltitudeChangeLevel3))

                )) {
                    // compute ascent
                    // always remember that accuracy is 3x worth on altitude than on latitude/longitude
                    deltaAscent = Math.floor(location.getAltitude() - lastGoodAscentLocation.getAltitude());
                    Logger("alt:" + lastGoodAscentLocation.getAltitude() + "->" + location.getAltitude() + ":" + deltaAscent + " - acc: " + location.getAccuracy());
                    
                    lastGoodAscentLocation = location;
                }

                _elapsedTime += deltaTime;
                _distance += deltaDistance;
                if (deltaAscent > 0) {
                    _ascent += deltaAscent;
                }
                
                _averageSpeed = (float) _distance / ((float) _elapsedTime / 1000f);

                nbGoodLocations++;

                Logger(location.getTime()/1000+ " deltaDistance:" + deltaDistance + " deltaTime:" + deltaTime + " deltaAscent:" + deltaAscent + " _ascent:" + _ascent);
                Logger("_distance: " + _distance + " _averageSpeed: " + _averageSpeed + " _elapsedTime:" + _elapsedTime);

                // additional conditions to compute statistics
                if (
                      (_distance == 0) // 1st location
                    ||
                      (deltaTime >= _minDeltaTimeToSaveLocation)
                    ||
                      (deltaDistance >= _minDeltaDistanceToSaveLocation)
                ) {
                    // this Location could be saved
                    //Logger("save Location");

                    // TODO
                }
                
            } // additional conditions to compute statistics
            
            lastGoodLocation = location;
            
        } // if (location.getAccuracy() <= _minAccuracy) {
        
        lastLocation = location;
    }
    
    // log functions
    public void Logger(String s) {
        Logger(s, LoggerType.LOG);
    }
    private enum LoggerType { LOG, TOAST };  
    public void Logger(String s, LoggerType type) {
        if (type == LoggerType.TOAST) {
            if (this._context != null) {
                Toast.makeText(this._context, s, Toast.LENGTH_LONG).show();
            }
            Log.v("JayPS-" + TAG, s);
        } else {            
            Log.v("JayPS-" + TAG, s);
        }
    }
}