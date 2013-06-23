package com.njackson;

import static com.getpebble.android.kit.Constants.MSG_DATA;

import java.text.DecimalFormat;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import fr.jayps.android.AdvancedLocation;

/**
 * Created with IntelliJ IDEA.
 * User: njackson
 * Date: 23/05/2013
 * Time: 13:30
 * To change this template use File | Settings | File Templates.
 */
public class GPSService extends Service {

    private int _updates;
    private float _speed;
    private float _averageSpeed;
    private float _distance;
    private long _elapsedTime;

    private float _prevspeed = -1;
    private float _prevaverageSpeed = -1;
    private float _prevdistance = -1;
    private double _currentLat;
    private double _currentLon;

    private AdvancedLocation _myLocation;

    private static float _speedConversion = 0.0f;
    private static float _distanceConversion = 0.0f;
    private static GPSService _this;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleCommand(intent);
        _this = this;
        
        makeServiceForeground("Pebble Bike", "GPS started");
        
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }
    
    @Override
    public void onCreate() {
        _locationMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        _locationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, onLocationChange);

        super.onCreate();
    }

    @Override
    public void onDestroy (){
        Log.d("GPSService","Stopped GPS Service");
        // save the state
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME,0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat("GPS_SPEED", _speed / _speedConversion);
        editor.putFloat("GPS_DISTANCE",_distance / _distanceConversion);
        editor.putLong("GPS_ELAPSEDTIME",_elapsedTime);
        editor.putInt("GPS_UPDATES", _updates);
        editor.commit();

        removeServiceForeground();
        
        PebbleKit.closeAppOnPebble(getApplicationContext(), Constants.WATCH_UUID);

        _locationMgr.removeUpdates(onLocationChange);
    }

    private void handleCommand(Intent intent) {
        Log.d("GPSService","Started GPS Service");

        // set the units to be used
        int units = intent.getIntExtra("UNITS",1);
        setConversionUnits(units);

        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME,0);
        _speed = settings.getFloat("GPS_SPEED",0);
        _distance = settings.getFloat("GPS_DISTANCE",0);
        _elapsedTime =  settings.getLong("GPS_ELAPSEDTIME",0);

        try {
        _updates = settings.getInt("GPS_UPDATES",0);
        }catch (ClassCastException e) {
            _updates = 0;
        }

        _myLocation = new AdvancedLocation(getApplicationContext());
        _myLocation.debugLevel = 1;
        _myLocation.setElapsedTime(_elapsedTime);
        _myLocation.setDistance(_distance);

        PebbleKit.startAppOnPebble(getApplicationContext(), Constants.WATCH_UUID);
    }

    public static void setConversionUnits(int units) {
        if(units == Constants.IMPERIAL) {
            _speedConversion = (float)Constants.MS_TO_MPH;
            _distanceConversion = (float)Constants.M_TO_MILES;
        } else {
            _speedConversion = (float)Constants.MS_TO_KPH;
            _distanceConversion = (float)Constants.M_TO_KM;
        }
    }

    public static void resetGPSStats(){
        if(_this == null)
            return;
        SharedPreferences settings = _this.getSharedPreferences(Constants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat("GPS_SPEED", 0.0f);
        editor.putFloat("GPS_DISTANCE",0.0f);
        editor.putFloat("GPS_AVGSPEED",0.0f);
        editor.putLong("GPS_ELAPSEDTIME",0);
        editor.putInt("GPS_UPDATES", 0);
        editor.commit();
        _this._myLocation = new AdvancedLocation(_this.getApplicationContext());
        _this._myLocation.debugLevel = 1;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
    
    private LocationManager _locationMgr = null;
    private LocationListener onLocationChange = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            _myLocation.onLocationChanged(location);
            
            Log.d("GPSService", "Got Speed: " + _myLocation.getSpeed() + " Accuracy: " + _myLocation.getAccuracy());

            _speed = _myLocation.getSpeed() * _speedConversion;

            if(_speed < 1) {
                _speed = 0;
            } else {
                _updates++;
            }

            _averageSpeed = _myLocation.getAverageSpeed() * _speedConversion;
            _elapsedTime = _myLocation.getElapsedTime();
            _distance = _myLocation.getDistance() * _distanceConversion;

            _currentLat = location.getLatitude();
            _currentLon = location.getLongitude();

            //if(_myLocation.getAccuracy() < 15.0) // not really needed, something similar is done in AdvancedLocation
            updatePebble();
    
        }

        @Override
        public void onProviderDisabled(String arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onProviderEnabled(String arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
            // TODO Auto-generated method stub
            
        }        
    };

    private void updatePebble() {

        if(_speed != _prevspeed || _averageSpeed != _prevaverageSpeed || _distance != _prevdistance) {

            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(MainActivity.GPSServiceReceiver.ACTION_RESP);
            broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
            broadcastIntent.putExtra("SPEED", _speed);
            broadcastIntent.putExtra("DISTANCE", _distance);
            broadcastIntent.putExtra("AVGSPEED", _averageSpeed);
            broadcastIntent.putExtra("LAT",_currentLat );
            broadcastIntent.putExtra("LON",_currentLon );
            sendBroadcast(broadcastIntent);

            _prevaverageSpeed = _averageSpeed;
            _prevdistance = _distance;
            _prevspeed = _speed;

            Log.d("GPSService:Pebble","Sending Data: _speed:" + _speed + " _distance: " + _distance + " _averageSpeed:"  + _averageSpeed);
            
            DecimalFormat df = new DecimalFormat("#.#");
            PebbleDictionary dic = new PebbleDictionary();
            dic.addString(Constants.SPEED_TEXT,df.format(_speed));
            dic.addString(Constants.DISTANCE_TEXT,df.format(_distance));
            dic.addString(Constants.AVGSPEED_TEXT,df.format(_averageSpeed));
            //dic.addInt32(Constants.STATE_CHANGED,Constants.STATE_START);
            
            dic.addString(Constants.ALTITUDE_TEXT,   String.format("%d", (int) _myLocation.getAltitude()));
            dic.addString(Constants.ASCENT_TEXT,     String.format("%d", (int) _myLocation.getAscent())); // 
            dic.addString(Constants.ASCENTRATE_TEXT, String.format("%d", (int) (3600f * _myLocation.getAscentRate()))); // in m/h
            dic.addString(Constants.SLOPE_TEXT,      String.format("%d", (int) (100f * _myLocation.getSlope()))); // 100%
            dic.addString(Constants.ACCURACY_TEXT,   String.format("%d", (int) _myLocation.getAccuracy()));
            
            Log.d("GPSService:Pebble","Sending Pebble ALTITUDE_TEXT: "      + String.format("%d", (int) _myLocation.getAltitude()));
            Log.d("GPSService:Pebble","Sending Pebble ASCENT_TEXT: "        + String.format("%d", (int) _myLocation.getAscent())); // 
            Log.d("GPSService:Pebble","Sending Pebble ASCENTRATE_TEXT: "    + String.format("%d", (int) (3600f * _myLocation.getAscentRate()))); // in m/h
            Log.d("GPSService:Pebble","Sending Pebble SLOPE_TEXT: "         + String.format("%d", (int) (100f * _myLocation.getSlope())));
            Log.d("GPSService:Pebble","Sending Pebble ACCURACY_TEXT: "  + String.format("%d", (int) _myLocation.getAccuracy()));
            Log.d("GPSService:Pebble", dic.toJsonString());
            PebbleKit.sendDataToPebble(getApplicationContext(), Constants.WATCH_UUID, dic);            
        }
    }
    private void makeServiceForeground(String titre, String texte) {
        //http://stackoverflow.com/questions/3687200/implement-startforeground-method-in-android
        final int myID = 1000;

        //The intent to launch when the user clicks the expanded notification
        Intent i = new Intent(this, MainActivity.class);

        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendIntent = PendingIntent.getActivity(this, 0, i, 0);

        // The following code is deprecated since API 11 (Android 3.x). Notification.Builder could be used instead, but without Android 2.x compatibility 
        Notification notification = new Notification(R.drawable.ic_launcher, "Pebble Bike", System.currentTimeMillis());
        notification.setLatestEventInfo(this, titre, texte, pendIntent);

        notification.flags |= Notification.FLAG_NO_CLEAR;

        startForeground(myID, notification);
    }
    private void removeServiceForeground() {
        stopForeground(true);
    }
}
