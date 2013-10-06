package com.njackson;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

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
	
	private static final String TAG = "PB-GPSService";

    private int _updates;
    private float _speed;
    private float _averageSpeed;
    private float _distance;

    private float _prevspeed = -1;
    private float _prevaverageSpeed = -1;
    private float _prevdistance = -1;
    private double _prevaltitude = -1;
    private long _prevtime = -1;
    private long _lastSaveGPSTime = 0;
    private double _currentLat;
    private double _currentLon;
    double xpos = 0;
    double ypos = 0;
    Location firstLocation = null;
    private AdvancedLocation _myLocation;
    private LiveTracking _liveTracking;
    
    private static GPSService _this;
    
    private int _refresh_interval = 1000;
    private boolean _gpsStarted = false;

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

        

        super.onCreate();
    }

    private boolean checkGPSEnabled(LocationManager locationMgr) {

        if(!locationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
           return false;
        } else {
            return true;
        }

    }

    @Override
    public void onDestroy (){
        Log.d(TAG, "Stopped GPS Service");
        
        saveGPSStats();

        _this = null;
        
        removeServiceForeground();
        
        //PebbleKit.closeAppOnPebble(getApplicationContext(), Constants.WATCH_UUID);

        _locationMgr.removeUpdates(onLocationChange);
        
    }

    // load the saved state
    public void loadGPSStats() {
    	Log.d(TAG, "loadGPSStats()");
    	
    	SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME,0);
        _speed = settings.getFloat("GPS_SPEED",0.0f);
        _distance = settings.getFloat("GPS_DISTANCE",0.0f);
        _myLocation.setDistance(_distance);
        _myLocation.setElapsedTime(settings.getLong("GPS_ELAPSEDTIME", 0));
        
        try {
            _myLocation.setAscent(settings.getFloat("GPS_ASCENT", 0.0f));
        } catch (ClassCastException e) {
            _myLocation.setAscent(0.0);
        }
        try {
            _updates = settings.getInt("GPS_UPDATES",0);
        } catch (ClassCastException e) {
            _updates = 0;
        }
        
        if (settings.contains("GPS_FIRST_LOCATION_LAT") && settings.contains("GPS_FIRST_LOCATION_LON")) {
            firstLocation = new Location("PebbleBike");
            firstLocation.setLatitude(settings.getFloat("GPS_FIRST_LOCATION_LAT", 0.0f));
            firstLocation.setLongitude(settings.getFloat("GPS_FIRST_LOCATION_LON", 0.0f));
        } else {
            firstLocation = null;
        }
        
    }

    // save the state
    public void saveGPSStats() {
    	Log.d(TAG, "saveGPSStats()");
    	
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME,0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat("GPS_SPEED", _speed);
        editor.putFloat("GPS_DISTANCE",_distance);
        editor.putLong("GPS_ELAPSEDTIME", _myLocation.getElapsedTime());
        editor.putFloat("GPS_ASCENT", (float) _myLocation.getAscent());
        editor.putInt("GPS_UPDATES", _updates);
        if (firstLocation != null) {
            editor.putFloat("GPS_FIRST_LOCATION_LAT", (float) firstLocation.getLatitude());
            editor.putFloat("GPS_FIRST_LOCATION_LON", (float) firstLocation.getLongitude());
        }
        editor.commit();
    }

    // reset the saved state
    public static void resetGPSStats(SharedPreferences settings) {
    	Log.d(TAG, "resetGPSStats()");
    	
	    SharedPreferences.Editor editor = settings.edit();
	    editor.putFloat("GPS_SPEED", 0.0f);
	    editor.putFloat("GPS_DISTANCE",0.0f);
	    editor.putLong("GPS_ELAPSEDTIME", 0);
	    editor.putFloat("GPS_ASCENT", 0.0f);
	    editor.putInt("GPS_UPDATES", 0);
        editor.remove("GPS_FIRST_LOCATION_LAT");
        editor.remove("GPS_FIRST_LOCATION_LON");
	    editor.commit();
	    
	    if (_this != null) {
	    	// GPS is running
		    // reninit all properties
	    	_this._myLocation = new AdvancedLocation(_this.getApplicationContext());
	    	_this._myLocation.debugLevel = 1;
	    	_this._myLocation.debugTagPrefix = "PB-";

	    	_this.loadGPSStats();  	    	
	    }
    }
    public static void changeRefreshInterval(int refresh_interval) {
        if (_this != null) {
            // GPS is running
            _this._refresh_interval = refresh_interval;
            _this._requestLocationUpdates(refresh_interval);
        }
    }

    /*public static void liveSendNames(int live_max_name) {
        Log.d(TAG, "liveSendNames("+live_max_name+")");
        if (_this != null) {
            // GPS is running

            String[] names = _this._liveTracking.getNames();
            
            //for (int i = 0; i < names.length; i++ ) {
            //    Log.d(TAG, "names["+i+"]: " + names[i]);
            //}
            PebbleDictionary dic = new PebbleDictionary();
            if (live_max_name < 0 && names[0] != null) {
                dic.addString(Constants.MSG_LIVE_NAME0, names[0]);
            }
            if (live_max_name < 1 && names[1] != null) {
                dic.addString(Constants.MSG_LIVE_NAME1, names[1]);
            }
            if (live_max_name < 2 && names[2] != null) {
                dic.addString(Constants.MSG_LIVE_NAME2, names[2]);
            }
            if (live_max_name < 3 && names[3] != null) {
                dic.addString(Constants.MSG_LIVE_NAME3, names[3]);
            }
            if (live_max_name < 4 && names[4] != null) {
                dic.addString(Constants.MSG_LIVE_NAME4, names[4]);
            }
            PebbleKit.sendDataToPebble(_this.getApplicationContext(), Constants.WATCH_UUID, dic);
            
            Log.d(TAG, "send MSG_LIVE_NAMEs");
        }
    }*/
    private void handleCommand(Intent intent) {
        Log.d(TAG, "Started GPS Service");
        
        _liveTracking = new LiveTracking(getApplicationContext());

        _myLocation = new AdvancedLocation(getApplicationContext());
        _myLocation.debugLevel = 1;
        _myLocation.debugTagPrefix = "PB-";

        loadGPSStats();
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        	 
        _liveTracking.setLogin(prefs.getString("LIVE_TRACKING_LOGIN", ""));
        _liveTracking.setPassword(prefs.getString("LIVE_TRACKING_PASSWORD", ""));
        _liveTracking.setUrl(prefs.getString("LIVE_TRACKING_URL", ""));

        // check to see if GPS is enabled
        if(checkGPSEnabled(_locationMgr)) {
            _requestLocationUpdates(intent.getIntExtra("REFRESH_INTERVAL", 1000));

            // send the saved values directly to update pebble
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(MainActivity.GPSServiceReceiver.ACTION_RESP);
            broadcastIntent.putExtra("DISTANCE", _myLocation.getDistance());
            broadcastIntent.putExtra("AVGSPEED", _myLocation.getAverageSpeed());
            broadcastIntent.putExtra("ASCENT",   _myLocation.getAscent());
            sendBroadcast(broadcastIntent);
        }else {
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(MainActivity.GPSServiceReceiver.ACTION_GPS_DISABLED);
            sendBroadcast(broadcastIntent);
            return;
        }

        
        
        //PebbleKit.startAppOnPebble(getApplicationContext(), Constants.WATCH_UUID);
    }
    private void _requestLocationUpdates(int refresh_interval) {
        Log.d(TAG, "_requestLocationUpdates("+refresh_interval+")");
        _refresh_interval = refresh_interval;

        if (_gpsStarted) {
            _locationMgr.removeUpdates(onLocationChange);
        }
        _locationMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, _refresh_interval, 2, onLocationChange);
        _gpsStarted = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
    
    private LocationManager _locationMgr = null;
    private LocationListener onLocationChange = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            int resultOnLocationChanged = _myLocation.onLocationChanged(location);
            
            //Log.d(TAG,  "onLocationChanged: " + _myLocation.getTime() + " Accuracy: " + _myLocation.getAccuracy());

            _speed = _myLocation.getSpeed();

            if(_speed < 1) {
                _speed = 0;
            } else {
                _updates++;
            }

            _averageSpeed = _myLocation.getAverageSpeed();
            _distance = _myLocation.getDistance();

            _currentLat = location.getLatitude();
            _currentLon = location.getLongitude();
            
            if (firstLocation == null) {
                firstLocation = location;
            }

            xpos = firstLocation.distanceTo(location) * Math.sin(firstLocation.bearingTo(location)/180*3.1415);
            ypos = firstLocation.distanceTo(location) * Math.cos(firstLocation.bearingTo(location)/180*3.1415); 

            xpos = Math.floor(xpos/10);
            ypos = Math.floor(ypos/10);
            Log.d(TAG,  "xpos="+xpos+"-ypos="+ypos);

            boolean send = false;
            //if(_myLocation.getAccuracy() < 15.0) // not really needed, something similar is done in AdvancedLocation
            if (_speed != _prevspeed || _averageSpeed != _prevaverageSpeed || _distance != _prevdistance || _prevaltitude != _myLocation.getAltitude()) {

                send = true;

                _prevaverageSpeed = _averageSpeed;
                _prevdistance = _distance;
                _prevspeed = _speed;
                _prevaltitude = _myLocation.getAltitude();
                _prevtime = _myLocation.getTime();
            } else if (_prevtime + 5000 < _myLocation.getTime()) {
                Log.d(TAG,  "New GPS data without move");
                
                send = true;
                
                _prevtime = _myLocation.getTime();
            }
            if (send) {
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(MainActivity.GPSServiceReceiver.ACTION_RESP);
                broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
                broadcastIntent.putExtra("SPEED", _speed);
                broadcastIntent.putExtra("DISTANCE", _distance);
                broadcastIntent.putExtra("AVGSPEED", _averageSpeed);
                broadcastIntent.putExtra("LAT",_currentLat);
                broadcastIntent.putExtra("LON",_currentLon);
                broadcastIntent.putExtra("ALTITUDE",   _myLocation.getAltitude()); // m
                broadcastIntent.putExtra("ASCENT",     _myLocation.getAscent()); // m
                broadcastIntent.putExtra("ASCENTRATE", (3600f * _myLocation.getAscentRate())); // in m/h
                broadcastIntent.putExtra("SLOPE",      (100f * _myLocation.getSlope())); // in %
                broadcastIntent.putExtra("ACCURACY",   _myLocation.getAccuracy()); // m
                broadcastIntent.putExtra("TIME",_myLocation.getElapsedTime());
                broadcastIntent.putExtra("XPOS", xpos);
                broadcastIntent.putExtra("YPOS", ypos);
                broadcastIntent.putExtra("BEARING", _myLocation.getBearing());
                sendBroadcast(broadcastIntent);

                if (_lastSaveGPSTime == 0 || (_myLocation.getTime() - _lastSaveGPSTime > 60000)) {
                    saveGPSStats();
                    _lastSaveGPSTime = _myLocation.getTime();
                }
            }

            if (MainActivity._liveTracking && resultOnLocationChanged == AdvancedLocation.SAVED) {
                _liveTracking.addPoint(firstLocation, location);
            }
            
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
