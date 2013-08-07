package com.njackson;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.maps.model.LatLng;

import de.cketti.library.changelog.ChangeLog;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends SherlockFragmentActivity  implements  GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, HomeActivity.OnButtonPressListener {
	
	private static final String TAG = "PB-MainActivity";

    private ActivityRecognitionClient _mActivityRecognitionClient;

    private static boolean _activityRecognition = false;
    public static boolean _liveTracking = false;
    private PendingIntent _callbackIntent;
    private RequestType _requestType;
    private static int _units = Constants.IMPERIAL;

    private static float _speedConversion = 0.0f;
    private static float _distanceConversion = 0.0f;
    private static float _altitudeConversion = 0.0f;
    
    private Date _lastCycling;

    private ActivityRecognitionReceiver _activityRecognitionReceiver;
    private GPSServiceReceiver _gpsServiceReceiver;
    private boolean _googlePlayInstalled;
    private Fragment _mapFragment;

    private int[] _altitudeBins;
    private int _altitudeMax = 0;
    private int _altitudeMin = 0;


    enum RequestType {
        START,
        STOP
    }

    // Listener for the fragment button press
    @Override
    public void onPressed(int sender, boolean value) {
        //To change body of implemented methods use File | Settings | File Templates.
        switch(sender) {
//            case R.id.MAIN_AUTO_START_BUTTON:
//                autoStartButtonClick(value);
//                break;
            case R.id.MAIN_START_BUTTON:
                startButtonClick(value);
                break;
//            case R.id.MAIN_UNITS_BUTTON:
//                unitsButtonClick(value);
//                break;
//            case R.id.MAIN_LIVE_TRACKING_BUTTON:
//                liveTrackingButtonClick(value);
//                break;
//            case R.id.MAIN_INSTALL_WATCHFACE_BUTTON:
//                sendWatchFaceToPebble();
//                break;
        }
    }
    public void loadPreferences() {
    	loadPreferences(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
    }
    public static void loadPreferences(SharedPreferences prefs) {
        //setup the defaults
        _activityRecognition = prefs.getBoolean("ACTIVITY_RECOGNITION",false);
        _liveTracking = prefs.getBoolean("LIVE_TRACKING",false);
        try {
        	setConversionUnits(Integer.valueOf(prefs.getString("UNITS_OF_MEASURE", "0")));
        } catch (Exception e) {
        	Log.e(TAG, "Exception:" + e);
        }
    }

    private void autoStartButtonClick(boolean value) {
        _activityRecognition = value;
        if(value) {
            initActivityRecognitionClient();
        }else {
            stopActivityRecogntionClient();
        }
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("ACTIVITY_RECOGNITION",_activityRecognition);
        editor.commit();
    }
    private void liveTrackingButtonClick(boolean value) {
    	_liveTracking = value;
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("LIVE_TRACKING", _liveTracking);
        editor.commit();
    }  
    private void startButtonClick(boolean value) {
        if(value) {
            startGPSService();
        } else {
            stopGPSService();
        }
    }
    public static void setConversionUnits(int units) {
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
    }

//    private void unitsButtonClick(boolean value) {
//        if (value) {
//            setConversionUnits(Constants.IMPERIAL);
//        } else {
//            setConversionUnits(Constants.METRIC);
//        }
//        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
//        SharedPreferences.Editor editor = settings.edit();
//        editor.putInt("UNITS_OF_MEASURE",_units);
//        editor.commit();
//        resendLastDataToPebble();
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

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
        bundle.putBoolean("LIVE_TRACKING",_liveTracking);
        bundle.putInt("UNITS_OF_MEASURE",_units);

        //instantiate the map fragment and store for future use
        //_mapFragment = Fragment.instantiate(this, "map", bundle);

        actionBar.addTab(actionBar.newTab().setText(R.string.TAB_TITLE_HOME).setTabListener(new TabListener<HomeActivity>(this, "home", HomeActivity.class, bundle)));
        //actionBar.addTab(actionBar.newTab().setText(R.string.TAB_TITLE_MAP).setTabListener(new TabListener<MapActivity>(this,"map",MapActivity.class,_mapFragment,null)));

        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("button")) {
            Log.d(TAG, "onCreate() button:" + getIntent().getExtras().getInt("button"));
            
            changeState(getIntent().getExtras().getInt("button"));
        }

        _altitudeBins = new int[14];
    }

    @Override
    public void onResume() {
        super.onResume();
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
        if (intent.getExtras() != null && intent.getExtras().containsKey("button")) {
            Log.d(TAG, "onNewIntent() button:" + intent.getExtras().getInt("button"));
            
            changeState(intent.getExtras().getInt("button"));
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

    private void sendWatchFaceToPebble(){
        try {
            Uri uri = Uri.parse("http://labs.jayps.fr/pebblebike/pebblebike-1.2.0.pbw?and");
            Intent startupIntent = new Intent();
            startupIntent.setAction(Intent.ACTION_VIEW);
            startupIntent.setType("application/octet-stream");
            startupIntent.setData(uri);
            ComponentName distantActivity = new ComponentName("com.getpebble.android", "com.getpebble.android.ui.UpdateActivity");
            startupIntent.setComponent(distantActivity);
            startActivity(startupIntent);
        }catch (ActivityNotFoundException ae) {
            Toast.makeText(getApplicationContext(),"Unable to install watchface, do you have the latest pebble app installed?",Toast.LENGTH_LONG).show();
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
        PebbleKit.sendDataToPebble(getApplicationContext(), Constants.WATCH_UUID, dic);
    }
    
    private Intent _lastIntent = null;
    private void resendLastDataToPebble() {
        sendDataToPebble(_lastIntent);
    }
    public void sendDataToPebble(Intent intent) {
    	Log.d(TAG, "sendDataToPebble()");
        
        PebbleDictionary dic = new PebbleDictionary();
        String sending = "Sending ";
        
        if (intent != null) {
            _lastIntent = intent;

            // force Locale.US to force dot as a decimal separator
            if (intent.hasExtra("SPEED")) {
                dic.addString(Constants.SPEED_TEXT,      String.format(Locale.US, "%.1f", intent.getFloatExtra("SPEED", 99) * _speedConversion)); // km/h or mph
                sending += " SPEED: "   + dic.getString(Constants.SPEED_TEXT);
            }
            if (intent.hasExtra("DISTANCE")) {
                dic.addString(Constants.DISTANCE_TEXT,   String.format(Locale.US, "%.1f", Math.floor(10 * intent.getFloatExtra("DISTANCE", 99) * _distanceConversion) / 10)); // km or miles
                sending += " DISTANCE: "   + dic.getString(Constants.DISTANCE_TEXT);
            }
            if (intent.hasExtra("AVGSPEED")) {
                dic.addString(Constants.AVGSPEED_TEXT,   String.format(Locale.US, "%.1f", intent.getFloatExtra("AVGSPEED", 99) * _speedConversion)); // km/h or mph
                sending += " AVGSPEED: "   + dic.getString(Constants.AVGSPEED_TEXT);
            }
            if (intent.hasExtra("ALTITUDE")) {
                dic.addString(Constants.ALTITUDE_TEXT,   String.format("%d", (int) (intent.getDoubleExtra("ALTITUDE", 99) * _altitudeConversion))); // m of ft
                sending += " ALTITUDE: "   + dic.getString(Constants.ALTITUDE_TEXT);
            }
            if (intent.hasExtra("ASCENT")) {
                dic.addString(Constants.ASCENT_TEXT,     String.format("%d", (int) (intent.getDoubleExtra("ASCENT", 99) * _altitudeConversion))); // m of ft
                sending += " ASCENT: "   + dic.getString(Constants.ASCENT_TEXT);
            }
            if (intent.hasExtra("ASCENTRATE")) {
                dic.addString(Constants.ASCENTRATE_TEXT, String.format("%d", (int) (intent.getFloatExtra("ASCENTRATE", 99) * _altitudeConversion))); // m/h or ft/h
                sending += " ASCENTRATE: "   + dic.getString(Constants.ASCENTRATE_TEXT);
            }
            if (intent.hasExtra("SLOPE")) {
                dic.addString(Constants.SLOPE_TEXT,      String.format("%d", (int) intent.getFloatExtra("SLOPE", 99))); // %
                sending += " SLOPE: "   + dic.getString(Constants.SLOPE_TEXT);
            }
            if (intent.hasExtra("ACCURACY")) {
                dic.addString(Constants.ACCURACY_TEXT,   String.format("%d", (int) intent.getFloatExtra("ACCURACY", 99))); // m
                sending += " ACCURACY: "   + dic.getString(Constants.ACCURACY_TEXT);
            }
        }
        dic.addInt32(Constants.MEASUREMENT_UNITS, _units);
        sending += " MEASUREMENT_UNITS: "   + dic.getInteger(Constants.MEASUREMENT_UNITS);
        
        if (checkServiceRunning()) {
            dic.addInt32(Constants.STATE_CHANGED,Constants.STATE_START);
        } else {
            dic.addInt32(Constants.STATE_CHANGED,Constants.STATE_STOP);
        }
        sending += " STATE_CHANGED: "   + dic.getInteger(Constants.STATE_CHANGED);

        Log.d(TAG, sending);
        PebbleKit.sendDataToPebble(getApplicationContext(), Constants.WATCH_UUID, dic);        
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
        if (intent.hasExtra("TIME")) {
            long time = intent.getLongExtra("TIME",0);
            Date date = new Date(time);
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
            String dateFormatted = formatter.format(date);
            homeScreen.setTime(dateFormatted);
        }
        if (intent.hasExtra("ALTITUDE")) {
            int altitude = (int)intent.getDoubleExtra("ALTITUDE", 0);
            if(altitude > _altitudeMax)
                _altitudeMax = altitude;
            if(altitude < _altitudeMin)
                _altitudeMin = altitude;

            for(int n=0; n < _altitudeBins.length-1;n++){
                if(_altitudeBins[n +1] > 0) {
                    if(_altitudeBins[n] > 0)
                        _altitudeBins[n] = (_altitudeBins[n] + _altitudeBins[n+1]) /2;
                    else
                        _altitudeBins[n] = _altitudeBins[n+1];
                }
            }

            _altitudeBins[13] = altitude;
            homeScreen.setAltitude(_altitudeBins,_altitudeMax,_altitudeMin);

        }
    }

    private void ResetSavedGPSStats() {
    	GPSService.resetGPSStats(getSharedPreferences(Constants.PREFS_NAME, 0));
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
        if(checkServiceRunning())
            return;

        //if(!_googlePlayInstalled) {
        //    Toast.makeText(getApplicationContext(),"Please install google play services",10);
        //    return;
        //}

        Intent intent = new Intent(getApplicationContext(), GPSService.class);

        registerGPSServiceIntentReceiver();
        startService(intent);
        
        PebbleKit.startAppOnPebble(getApplicationContext(), Constants.WATCH_UUID);
        resendLastDataToPebble();
    }

    private void stopGPSService() {
        if(!checkServiceRunning())
            return;
        Log.d(TAG, "stopGPSService()");
        removeGPSServiceIntentReceiver();
        stopService(new Intent(getApplicationContext(), GPSService.class));
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
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(googlePlayServicesAvailable, this, 123);
            if(errorDialog != null)
                errorDialog.show();
            _googlePlayInstalled = false;
        } else {
            _googlePlayInstalled = true;
        }
    }

    private boolean checkServiceRunning() {

        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (GPSService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;

    }

    private void showGPSDisabledAlertToUser(){
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
        Intent intent = new Intent(getApplicationContext(), ActivityRecognitionIntentService.class);
        _callbackIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if(_requestType == RequestType.START) {
            Log.d(TAG, "Start Recognition");
            _mActivityRecognitionClient.requestActivityUpdates(30000, _callbackIntent);
        } else {
            Log.d(TAG, "Stop Recognition");
            _mActivityRecognitionClient.removeActivityUpdates(_callbackIntent);
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
                sendDataToPebble(intent);
                updateScreen(intent);
                //updateMapLocation(intent);
            } else if(intent.getAction().compareTo(ACTION_GPS_DISABLED) == 0) {
                stopGPSService();
                setStartButtonText("Start");
                showGPSDisabledAlertToUser();
            }

        }
    }

    private void updateMapLocation(Intent intent) {
        // do we need to update the map
        double lat =  intent.getDoubleExtra("LAT",0);
        double lon = intent.getDoubleExtra("LON",0);
        MapActivity activity = (MapActivity)getSupportFragmentManager().findFragmentByTag("map");
        if(activity != null)
            activity.setLocation(new LatLng(lat,lon));
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

}