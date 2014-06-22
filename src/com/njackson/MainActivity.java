package com.njackson;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.*;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.PebbleKit.FirmwareVersionInfo;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;

import com.njackson.util.AltitudeGraphReduce;
import de.cketti.library.changelog.ChangeLog;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends SherlockFragmentActivity  implements  GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, HomeActivity.OnButtonPressListener {
	
	private static final String TAG = "PB-MainActivity";

    private ActivityRecognitionClient _mActivityRecognitionClient;

    private static boolean _activityRecognition = false;
    public static boolean _liveTrackingJayps = false;
    public static boolean _liveTrackingMmt = false;
    public static String oruxmaps_autostart = "disable";
    
    public static String hrm_name = "";
    public static String hrm_address = "";
    public static int pebbleFirmwareVersion = 0;
    public static FirmwareVersionInfo pebbleFirmwareVersionInfo;
    
    private PendingIntent _callbackIntent;
    private RequestType _requestType;
    private static int _units = Constants.IMPERIAL;
    
    private long _sendDataToPebbleLastTime = 0;
    private static int _refresh_interval = 1000;
    // Height of geoid above WGS84 ellipsoid
    public static double geoidHeight = 0; // in m
    public static int batteryLevel = -1;
    public static boolean debug = false;

    private static float _speedConversion = 0.0f;
    private static float _distanceConversion = 0.0f;
    private static float _altitudeConversion = 0.0f;
    
    private Date _lastCycling;

    private ActivityRecognitionReceiver _activityRecognitionReceiver;
    private GPSServiceReceiver _gpsServiceReceiver;
    private boolean _googlePlayInstalled;

    public static VirtualPebble virtualPebble;

    enum RequestType {
        START,
        STOP
    }

    static MainActivity instance;

    public static MainActivity getInstance() {
        return instance;
    }

    public Boolean activityRecognitionEnabled() {
        return _activityRecognition;
    }

    // Listener for the fragment button press
    @Override
    public void onPressed(int sender, boolean value) {
        //To change body of implemented methods use File | Settings | File Templates.
        switch(sender) {
            case R.id.MAIN_START_BUTTON:
                startButtonClick(value);
                break;
        }
    }

    public void loadPreferences() {
    	loadPreferences(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
    }

    public void loadPreferences(SharedPreferences prefs) {
        //setup the defaults

        debug = prefs.getBoolean("PREF_DEBUG", false);

        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME,0);

        hrm_name = settings.getString("hrm_name", "");
        hrm_address = settings.getString("hrm_address", "");
        if (debug) Log.d(TAG, "hrm_name:" + hrm_name + " " + hrm_address);

        _activityRecognition = prefs.getBoolean("ACTIVITY_RECOGNITION",false);
        _liveTrackingJayps = prefs.getBoolean("LIVE_TRACKING",false);
        _liveTrackingMmt = prefs.getBoolean("LIVE_TRACKING_MMT",false);
        oruxmaps_autostart = prefs.getString("ORUXMAPS_AUTO", "disable");

        if(_activityRecognition)
            initActivityRecognitionClient();
        else
            stopActivityRecogntionClient();

        HomeActivity activity = getHomeScreen();
        if(activity != null)
            activity.setStartButtonVisibility(!_activityRecognition);

        try {
        	setConversionUnits(Integer.valueOf(prefs.getString("UNITS_OF_MEASURE", "0")));
        } catch (Exception e) {
        	Log.e(TAG, "Exception:" + e);
        }
        try {
            int prev_refresh_interval = _refresh_interval;
            _refresh_interval = Integer.valueOf(prefs.getString("REFRESH_INTERVAL", "1000"));
            if (prev_refresh_interval != _refresh_interval) {
                GPSService.changeRefreshInterval(_refresh_interval);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception converting REFRESH_INTERVAL:" + e);
        }
    }

    private void startButtonClick(boolean value) {
        if(value) {
            startGPSService();
        } else {
            stopGPSService();
        }
    }
    public void setConversionUnits(int units) {
        _units = units;
        
        if(units == Constants.IMPERIAL) {
            _speedConversion = (float)Constants.MS_TO_MPH;
            _distanceConversion = (float)Constants.M_TO_MILES;
            _altitudeConversion = (float)Constants.M_TO_FEET;
        } else {
            _speedConversion = (float)Constants.MS_TO_KPH;
            _distanceConversion = (float)Constants.M_TO_KM;
            _altitudeConversion = (float)Constants.M_TO_M;
        }

        // set the screen units
        HomeActivity homeScreen = getHomeScreen();
        if(homeScreen != null)
            homeScreen.setUnits(units);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        instance = this;

        setContentView(R.layout.main);

        virtualPebble = new VirtualPebble(getApplicationContext());

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayHomeAsUpEnabled(false);

        ChangeLog cl = new ChangeLog(this);
        if (cl.isFirstRun()) {
          cl.getLogDialog().show();
        }
        
        checkGooglePlayServices();

        loadPreferences();

        Bundle bundle = new Bundle();
        bundle.putBoolean("ACTIVITY_RECOGNITION",_activityRecognition);
        bundle.putBoolean("LIVE_TRACKING",_liveTrackingJayps);
        bundle.putBoolean("LIVE_TRACKING_MMT",_liveTrackingMmt);
        bundle.putInt("UNITS_OF_MEASURE",_units);


        actionBar.addTab(actionBar.newTab().setText(R.string.TAB_TITLE_HOME).setTabListener(new TabListener<HomeActivity>(this, "home", HomeActivity.class, bundle)));

        if (getIntent().getExtras() != null) {
            if (getIntent().getExtras().containsKey("button")) {
                Log.d(TAG, "onCreate() button:" + getIntent().getExtras().getInt("button"));
            
                changeState(getIntent().getExtras().getInt("button"));
            }
            if (getIntent().getExtras().containsKey("version")) {
                Log.d(TAG, "onCreate() version:" + getIntent().getExtras().getInt("version"));
                notificationVersion(getIntent().getExtras().getInt("version"));
                resendLastDataToPebble();
            }
            /*if (getIntent().getExtras().containsKey("live_max_name")) {
                Log.d(TAG, "onNewIntent() live_max_name:" + getIntent().getExtras().getInt("live_max_name"));
                GPSService.liveSendNames(getIntent().getExtras().getInt("live_max_name"));
            }*/
        }
        
        // try to get Pebble Watch Firmware version
        try {
            // getWatchFWVersion works only with firmware 2.x
            pebbleFirmwareVersionInfo = PebbleKit.getWatchFWVersion(getApplicationContext());
            pebbleFirmwareVersion = 2;
            if (pebbleFirmwareVersionInfo == null) {
                // if the watch is disconnected or we can't get the version
                Log.e(TAG, "pebbleFirmwareVersionInfo == null");
            } else {
                Log.e(TAG, "getMajor:"+pebbleFirmwareVersionInfo.getMajor());
                Log.e(TAG, "getMinor:"+pebbleFirmwareVersionInfo.getMinor());
                Log.e(TAG, "getPoint:"+pebbleFirmwareVersionInfo.getPoint());
                Log.e(TAG, "getTag:"+pebbleFirmwareVersionInfo.getTag());
            }
        } catch (Exception e) {
            //Log.e(TAG, "Exception getWatchFWVersion " + e.getMessage());
            // getWatchFWVersion works only with 2.x firmware
            pebbleFirmwareVersion = 1;
            pebbleFirmwareVersionInfo = null;
        }
        Log.d(TAG, "pebbleFirmwareVersion=" + pebbleFirmwareVersion);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (debug) Log.d(TAG, "onResume");
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (debug) Log.d(TAG, "onPause");
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
        	// Physical Menu button
        	startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
        }
        return super.onKeyUp(keyCode, event);
    }    
    
    // This is called for activities that set launchMode to "singleTop" in their package, or if a client used the FLAG_ACTIVITY_SINGLE_TOP flag when calling startActivity(Intent).
    // In either case, when the activity is re-launched while at the top of the activity stack instead of a new instance of the activity being started, onNewIntent() will be called 
    // on the existing instance with the Intent that was used to re-launch it.
    // An activity will always be paused before receiving a new intent, so you can count on onResume() being called after this method. 
    protected void onNewIntent (Intent intent) {
        if (intent.getExtras() != null) {
            if (intent.getExtras().containsKey("button")) {
                Log.d(TAG, "onNewIntent() button:" + intent.getExtras().getInt("button"));
            
                changeState(intent.getExtras().getInt("button"));
            }
            if (intent.getExtras().containsKey("version")) {
                Log.d(TAG, "onNewIntent() version:" + intent.getExtras().getInt("version"));
                notificationVersion(intent.getExtras().getInt("version"));
                resendLastDataToPebble();
            }
            /*if (intent.getExtras().containsKey("live_max_name")) {
                Log.d(TAG, "onNewIntent() live_max_name:" + intent.getExtras().getInt("live_max_name"));
                GPSService.liveSendNames(intent.getExtras().getInt("live_max_name"));
            }*/
        }
    }
    private void notificationVersion(int version) {
        if (version < Constants.LAST_VERSION_PEBBLE) {
            if (debug) Log.d(TAG, "version:" + version + " min:" + Constants.MIN_VERSION_PEBBLE + " last:" + Constants.LAST_VERSION_PEBBLE);
            String msg = "A new watchface is available. Please install it from the Pebble Bike android application settings";
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            if (version < Constants.MIN_VERSION_PEBBLE) {
                VirtualPebble.showSimpleNotificationOnPebble("Pebble Bike", msg);
            }
        }
    }
    private void changeState(int button) {
        Log.d(TAG, "changeState(button:" + button + ")");
        switch (button) {
            case Constants.STOP_PRESS:
                stopGPSService();
                setStartButtonText("Start");
                break;
            case Constants.PLAY_PRESS:
                startGPSService();
                setStartButtonText("Stop");
                break;
            case Constants.REFRESH_PRESS:
                ResetSavedGPSStats();
                break;
        }        
    }



    private void sendServiceState() {
    	Log.d(TAG, "sendServiceState()");
    	PebbleDictionary dic = new PebbleDictionary();
        if(checkServiceRunning()) {
            dic.addInt32(Constants.STATE_CHANGED,Constants.STATE_START);
        } else {
            dic.addInt32(Constants.STATE_CHANGED,Constants.STATE_STOP);
        }
        Log.d(TAG, " STATE_CHANGED: "   + dic.getInteger(Constants.STATE_CHANGED));
        VirtualPebble.sendDataToPebble(dic);
    }
    
    private Intent _lastIntent = null;
    private void resendLastDataToPebble() {
        sendBatteryLevel();
        sendDataToPebble(_lastIntent);
    }
    public void sendDataToPebble(Intent intent) {
    	//Log.d(TAG, "sendDataToPebble()");
        
        PebbleDictionary dic = new PebbleDictionary();
        String sending = "Sending ";
        boolean forceSend = false;
        
        if (intent == null) {
            Log.d(TAG, "sendDataToPebble(intent == null)");
            intent = new Intent();
            
            SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME,0);
            float distance = settings.getFloat("GPS_DISTANCE", 0.0f);
            intent.putExtra("DISTANCE", distance);
            
            dic.addInt32(Constants.MSG_VERSION_ANDROID, Constants.VERSION_ANDROID);
            sending += " MSG_VERSION_ANDROID: "   + dic.getInteger(Constants.MSG_VERSION_ANDROID);

            forceSend = true;
        }

        if (intent != null) {
            _lastIntent = intent;

            byte[] data = new byte[21];

            data[0] = (byte) ((_units % 2) * (1<<0));
            data[0] += (byte) ((checkServiceRunning() ? 1 : 0) * (1<<1));
            data[0] += (byte) ((debug ? 1 : 0) * (1<<2));
            data[0] += (byte) ((_liveTrackingJayps ? 1 : 0) * (1<<3));
            
            int refresh_code = 1; // 1s
            if (_refresh_interval < 1000) {
                refresh_code = 0; // [0;1[
            } else if (_refresh_interval >= 5000) {
                refresh_code = 3; // [5;+inf
            } else if (_refresh_interval > 1000) {
                refresh_code = 2; // ]1;5[
            }
            data[0] += (byte) ((refresh_code % 4) * (1<<4)); // 2 bits
            // unused bits
            data[0] += (byte) (0 * (1<<6));
            data[0] += (byte) (0 * (1<<7));
            //Log.d(TAG, _units+"|"+checkServiceRunning()+"|debug:"+debug+"|"+_liveTrackingJayps+"|_refresh_interval="+_refresh_interval+"|refresh_code="+refresh_code+"|"+((256+data[0])%256));
            
            data[1] = (byte) ((int)  Math.ceil(intent.getFloatExtra("ACCURACY", 0.0f)));
            data[2] = (byte) (((int) (Math.floor(100 * intent.getFloatExtra("DISTANCE", 0.0f) * _distanceConversion) / 1)) % 256);
            data[3] = (byte) (((int) (Math.floor(100 * intent.getFloatExtra("DISTANCE", 0.0f) * _distanceConversion) / 1)) / 256);
            data[4] = (byte) (((int) intent.getLongExtra("TIME", 0) / 1000) % 256);
            data[5] = (byte) (((int) intent.getLongExtra("TIME", 0) / 1000) / 256);

            data[6] = (byte) (((int) (intent.getDoubleExtra("ALTITUDE", 0) * _altitudeConversion)) % 256);
            data[7] = (byte) (((int) (intent.getDoubleExtra("ALTITUDE", 0) * _altitudeConversion)) / 256);

            data[8] = (byte) (((int) Math.abs(intent.getDoubleExtra("ASCENT", 0) * _altitudeConversion)) % 256);
            data[9] = (byte) ((((int) Math.abs(intent.getDoubleExtra("ASCENT", 0) * _altitudeConversion)) / 256) % 128);
            if (intent.getDoubleExtra("ASCENT", 0.0f) < 0) {
                data[9] += 128;
            }
            data[10] = (byte) (((int) Math.abs(intent.getFloatExtra("ASCENTRATE", 0.0f) * _altitudeConversion)) % 256);
            data[11] = (byte) ((((int) Math.abs(intent.getFloatExtra("ASCENTRATE", 0.0f) * _altitudeConversion)) / 256) % 128);
            if (intent.getFloatExtra("ASCENTRATE", 0.0f) < 0) {
                data[11] += 128;
            }            
            data[12] = (byte) (((int) Math.abs(intent.getFloatExtra("SLOPE", 0.0f))) % 128);
            if (intent.getFloatExtra("SLOPE", 0.0f) < 0) {
                data[12] += 128;
            }            

            data[13] = (byte) (((int) Math.abs(intent.getDoubleExtra("XPOS", 0))) % 256);
            data[14] = (byte) ((((int) Math.abs(intent.getDoubleExtra("XPOS", 0))) / 256) % 128);
            if (intent.getDoubleExtra("XPOS", 0) < 0) {
                data[14] += 128;
            }
            data[15] = (byte) (((int) Math.abs(intent.getDoubleExtra("YPOS", 0))) % 256);
            data[16] = (byte) ((((int) Math.abs(intent.getDoubleExtra("YPOS", 0))) / 256) % 128);
            if (intent.getDoubleExtra("YPOS", 0) < 0) {
                data[16] += 128;
            }

            data[17] = (byte) (((int) (Math.floor(10 * intent.getFloatExtra("SPEED", 0.0f) * _speedConversion) / 1)) % 256);
            data[18] = (byte) (((int) (Math.floor(10 * intent.getFloatExtra("SPEED", 0.0f) * _speedConversion) / 1)) / 256);
            data[19] = (byte) (((int)  (intent.getFloatExtra("BEARING", 0.0f) / 360 * 256)) % 256);
            data[20] = (byte) ((intent.getIntExtra("HEARTRATE", 255)) % 256);

            dic.addBytes(Constants.ALTITUDE_DATA, data);
            
            for( int i = 0; i < data.length; i++ ) {
                sending += " data["+i+"]: "   + ((256+data[i])%256);
            }
        }
        
        /*if (checkServiceRunning()) {
            dic.addInt32(Constants.STATE_CHANGED,Constants.STATE_START);
        } else {
            dic.addInt32(Constants.STATE_CHANGED,Constants.STATE_STOP);
        }
        sending += " STATE_CHANGED: "   + dic.getInteger(Constants.STATE_CHANGED);*/

        if (MainActivity.debug) Log.d(TAG, sending);
        VirtualPebble.sendDataToPebble(dic, forceSend);
    }

    private void updateScreen(Intent intent) {

        HomeActivity homeScreen = getHomeScreen();

        if (intent.hasExtra("SPEED")) {
            String speed = String.format(Locale.US, "%.1f", intent.getFloatExtra("SPEED", 99) * _speedConversion);
            homeScreen.setSpeed(speed);
        }
        if (intent.hasExtra("DISTANCE")) {
            String distance = String.format(Locale.US, "%.1f", Math.floor(10 * intent.getFloatExtra("DISTANCE", 99) * _distanceConversion) / 10);
            homeScreen.setDistance(distance);
        }
        if (intent.hasExtra("AVGSPEED")) {
            String avgSpeed = String.format(Locale.US, "%.1f", intent.getFloatExtra("AVGSPEED", 99) * _speedConversion);
            homeScreen.setAvgSpeed(avgSpeed);
        }
        if (intent.hasExtra("ACCURACY")) {
            String gpsStatus = "";
            float accuracy = intent.getFloatExtra("ACCURACY", 99.9f);
            if (accuracy <= 4) {
                gpsStatus = "EXCELLENT";
            } else if (accuracy <= 6) {
                gpsStatus = "GOOD";
            } else if (accuracy <= 10) {
                gpsStatus = "MEDIUM";
            } else {
                gpsStatus = "POOR";
            }
            //Log.d(TAG, "gpsStatus: " + accuracy + " => " + gpsStatus);
            homeScreen.setGPSStatus(gpsStatus);
        }
        if (intent.hasExtra("TIME")) {
            int time = (int) (intent.getLongExtra("TIME",0) / 1000);
            int s = time % 60;
            int m = ((time-s) / 60) % 60;
            int h = (time-s-60*m) / (60 * 60);
            
            String dateFormatted = String.format("%d:%02d:%02d", h, m, s);
            //Log.d(TAG, time + ":" + h+"/"+m+"/"+s + " " + dateFormatted);
            
            homeScreen.setTime(dateFormatted);
        }
        if (intent.hasExtra("ALTITUDE")) {
            int altitude = (int)intent.getDoubleExtra("ALTITUDE", 0);

            AltitudeGraphReduce alt = AltitudeGraphReduce.getInstance();
            alt.addAltitude(altitude, intent.getLongExtra("TIME",0), intent.getFloatExtra("DISTANCE", 0));

            homeScreen.setAltitude(
                    alt.getGraphData(),
                    alt.getMax(),
                    alt.getMin());

        }
    }

    public void ResetSavedGPSStats() {
    	GPSService.resetGPSStats(getSharedPreferences(Constants.PREFS_NAME, 0));
        AltitudeGraphReduce.getInstance().restData();
        
        // send the saved values directly to update pebble
        Intent intent = new Intent();
        intent.putExtra("DISTANCE", 0);
        intent.putExtra("AVGSPEED", 0);
        intent.putExtra("ASCENT",   0);
        sendDataToPebble(intent);
    }

    private void setStartButtonText(String text) {
        HomeActivity activity = getHomeScreen();
        if(activity != null)
            activity.setStartButtonText(text);
    }

    private HomeActivity getHomeScreen() {
        return (HomeActivity)(getSupportFragmentManager().findFragmentByTag("home"));
    }

    private void stopActivityRecogntionClient() {
        removeActivityRecognitionIntentReceiver();
        if(_mActivityRecognitionClient == null)
            _mActivityRecognitionClient = new ActivityRecognitionClient(getApplicationContext(), this, this);
        _requestType = RequestType.STOP;
        _mActivityRecognitionClient.connect();
    }

    private void initActivityRecognitionClient() {
        // Connect to the ActivityRecognitionService
        registerActivityRecognitionIntentReceiver();
        _requestType = RequestType.START;
        _mActivityRecognitionClient = new ActivityRecognitionClient(getApplicationContext(), this, this);
        _mActivityRecognitionClient.connect();
    }

    private void registerActivityRecognitionIntentReceiver() {
        IntentFilter filter = new IntentFilter(ActivityRecognitionReceiver.ACTION_RESP);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        _activityRecognitionReceiver = new ActivityRecognitionReceiver();
        registerReceiver(_activityRecognitionReceiver, filter);
    }

    private void removeActivityRecognitionIntentReceiver() {
        if(_activityRecognitionReceiver != null)
            unregisterReceiver(_activityRecognitionReceiver);
    }

    private void startGPSService() {
        if (!checkServiceRunning()) {
            // only if GPS was not running on the phone
            
            Intent intent = new Intent(getApplicationContext(), GPSService.class);
            intent.putExtra("REFRESH_INTERVAL", _refresh_interval);
    
            registerGPSServiceIntentReceiver();
            startService(intent);
            
            PebbleKit.startAppOnPebble(getApplicationContext(), Constants.WATCH_UUID);
        }
        // in all cases
        resendLastDataToPebble();
    }

    private void stopGPSService() {
        Log.d(TAG, "stopGPSService()");
        
        if (checkServiceRunning()) {
            // only if GPS was running on the phone
            
            removeGPSServiceIntentReceiver();
            stopService(new Intent(getApplicationContext(), GPSService.class));
        }
        // in all cases
        sendServiceState();
    }

    private void registerGPSServiceIntentReceiver() {
        IntentFilter filterResponse = new IntentFilter(GPSServiceReceiver.ACTION_RESP);
        filterResponse.addCategory(Intent.CATEGORY_DEFAULT);

        IntentFilter filterDisabled = new IntentFilter(GPSServiceReceiver.ACTION_GPS_DISABLED);
        filterDisabled.addCategory(Intent.CATEGORY_DEFAULT);

        _gpsServiceReceiver = new GPSServiceReceiver();
        registerReceiver(_gpsServiceReceiver, filterResponse);
        registerReceiver(_gpsServiceReceiver, filterDisabled);
    }

    private void removeGPSServiceIntentReceiver() {
        if(_gpsServiceReceiver != null)
            unregisterReceiver(_gpsServiceReceiver);
    }

    private void checkGooglePlayServices() {
        // check to see that google play services are installed and up to date
        int googlePlayServicesAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(googlePlayServicesAvailable !=  ConnectionResult.SUCCESS) {
            // google play services need to be updated
            try {
                Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(googlePlayServicesAvailable, this, 123);
                if(errorDialog != null)
                    errorDialog.show();
            } catch (NoClassDefFoundError e) {
                Log.d(TAG, "NoClassDefFoundError " + e.getMessage());
                Toast.makeText(this, "This device is not supported by Google Play Service.", Toast.LENGTH_LONG).show();                
            } catch (Exception e) {
                Log.e(TAG, "Exception " + e.getMessage());
            }
            _googlePlayInstalled = false;
        } else {
            _googlePlayInstalled = true;
        }
    }

    public boolean checkServiceRunning() {

        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (GPSService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;

    }

    private void showGPSDisabledAlertToUser(){
        VirtualPebble.showSimpleNotificationOnPebble("Pebble Bike", "GPS is disabled on your phone. Please enable it.");

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("GPS is disabled in your device. Would you like to enable it?")
                .setCancelable(false)
                .setPositiveButton("Goto Settings Page To Enable GPS",
                        new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int id){
                                Intent callGPSSettingIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(callGPSSettingIntent);
                            }
                        });
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int id){
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    private void updateActivityType(int type) {

        String activityType = "";
        switch(type) {
            case DetectedActivity.ON_BICYCLE:
                activityType = "Bicycle";
                break;
            case DetectedActivity.STILL:
                activityType = "Still";
                break;
            case DetectedActivity.IN_VEHICLE:
                activityType = "In Vehicle";
                break;
            case DetectedActivity.ON_FOOT:
                activityType = "On Foot";
                break;
        }

        HomeActivity activity = getHomeScreen();
        if(activity != null)
            activity.setActivityText(activityType);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected");
        Intent intent = new Intent(getApplicationContext(), ActivityRecognitionIntentService.class);
        _callbackIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if(_requestType == RequestType.START) {
            Log.d(TAG, "Start Recognition");
            _mActivityRecognitionClient.requestActivityUpdates(30000, _callbackIntent);
        } else if(_requestType == RequestType.STOP) {
            Log.d(TAG, "Stop Recognition");
            _mActivityRecognitionClient.removeActivityUpdates(_callbackIntent);
        } else {
            Log.d(TAG, "other?");
        }
    }

    @Override
    public void onDisconnected() {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public class ActivityRecognitionReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP =
                "com.njackson.intent.action.MESSAGE_PROCESSED";
        @Override
        public void onReceive(Context context, Intent intent) {

            int activity = intent.getIntExtra("ACTIVITY_CHANGED", 0);
            updateActivityType(activity);

            if(activity == DetectedActivity.ON_BICYCLE) {
                Log.d(TAG, "AutoStart");
                startGPSService();
                _lastCycling = new Date();
            } else {
                Log.d(TAG, "Waiting for stop");
                // check to see if we have been inactive for 2 minutes
                Calendar cal = Calendar.getInstance();
                cal.setTime(new Date());
                cal.add(Calendar.MINUTE, -2);
                if(_lastCycling == null || _lastCycling.before(cal.getTime())) {
                    Log.d(TAG, "AutoStop");
                    stopGPSService();
                    _lastCycling = null;
                }
            }

        }
    }

    public class GPSServiceReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP =
                "com.njackson.intent.action.UPDATE_PEBBLE";
        public static final String ACTION_GPS_DISABLED =
                "com.njackson.intent.action.GPS_DISABLED";
        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.getAction().compareTo(ACTION_RESP) == 0) {
                if (_sendDataToPebbleLastTime > 0 && (System.currentTimeMillis() - _sendDataToPebbleLastTime < _refresh_interval)) {
                    if (debug) Log.d(TAG, "skip sendDataToPebble");
                } else {
                    _sendDataToPebbleLastTime = System.currentTimeMillis();
                    sendDataToPebble(intent);
                }
                updateScreen(intent);
            } else if(intent.getAction().compareTo(ACTION_GPS_DISABLED) == 0) {
                stopGPSService();
                setStartButtonText("Start");
                showGPSDisabledAlertToUser();
            }

        }
    }

    public static class TabListener<T extends SherlockFragment> implements ActionBar.TabListener {
        private final SherlockFragmentActivity mActivity;
        private final String mTag;
        private final Class<T> mClass;
        private final Bundle mArgs;
        private android.support.v4.app.Fragment mFragment;

        public TabListener(SherlockFragmentActivity activity, String tag, Class<T> clz) {
            this(activity, tag, clz, null);
        }

        public TabListener(SherlockFragmentActivity activity, String tag, Class<T> clz,android.support.v4.app.Fragment fragment, Bundle args) {
            this(activity, tag, clz, args);
            mFragment = fragment;
        }

        public TabListener(SherlockFragmentActivity activity, String tag, Class<T> clz, Bundle args) {
            mActivity = activity;
            mTag = tag;
            mClass = clz;
            mArgs = args;

            // Check to see if we already have a fragment for this tab, probably
            // from a previously saved state.  If so, deactivate it, because our
            // initial state is that a tab isn't shown.
            mFragment = mActivity.getSupportFragmentManager().findFragmentByTag(mTag);
            if (mFragment != null && !mFragment.isDetached()) {
                FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
                ft.detach(mFragment);
                ft.commit();
            }
        }

        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            if (mFragment == null) {
                mFragment = Fragment.instantiate(mActivity, mClass.getName(), mArgs);
                //mFragment.setArguments(mArgs);
                ft.add(android.R.id.content, mFragment, mTag);
            } else {
                ft.attach(mFragment);
            }

        }

        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            if (mFragment != null) {
                ft.detach(mFragment);
            }
        }

        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
            //Do nothing
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.activity_action_bar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_settings :
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    public static void sendBatteryLevel() {
        if (debug) Log.d(TAG, "sendBatteryLevel:" + batteryLevel);
        
        PebbleDictionary dic = new PebbleDictionary();
        dic.addInt32(Constants.MSG_BATTERY_LEVEL, batteryLevel);
        VirtualPebble.sendDataToPebble(dic);
    }
}
