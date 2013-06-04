package com.njackson;

import android.app.ActivityManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;

import java.text.DecimalFormat;
import java.util.List;

public class MainActivity extends SherlockActivity implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener {

    private ActivityRecognitionClient _mActivityRecognitionClient;
    private ActivityRecognitionReceiver _activityRecognitionReceiver;
    private GPSServiceReceiver _gpsServiceReceiver;
    private boolean _activityRecognition = false;
    PendingIntent _callbackIntent;
    private RequestType _requestType;
    private PebbleKit.PebbleDataReceiver _pebbleDataHandler = null;
    private int _units = Constants.IMPERIAL;

    enum RequestType {
        START,
        STOP
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar ab = getSupportActionBar();
        //setTheme(ActionBar.LI);

        checkGooglePlayServices();
        checkRunkeeperRunning();
        setContentView(R.layout.main);

        Intent intent = new Intent(getApplicationContext(), ActivityRecognitionIntentService.class);

        _callbackIntent = PendingIntent.getService(getApplicationContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        final ToggleButton _autoStart = (ToggleButton)findViewById(R.id.MAIN_AUTO_START_BUTTON);
        final Button _startButton = (Button)findViewById(R.id.MAIN_START_BUTTON);
        final Button _watchfaceButton = (Button)findViewById(R.id.MAIN_INSTALL_WATCHFACE_BUTTON);
        final ToggleButton _unitsButton = (ToggleButton)findViewById(R.id.MAIN_UNITS_BUTTON);

        _autoStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (!_activityRecognition) {
                    initActivityRecognitionClient();
                    _startButton.setVisibility(View.GONE);
                }else {
                    stopActivityRecogntionClient();
                    _startButton.setVisibility(View.VISIBLE);
                }

                _activityRecognition = !_activityRecognition;

                SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME,0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("ACTIVITY_RECOGNITION",_activityRecognition);
                editor.commit();

            }
        });

        _unitsButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                _units = (_units == Constants.IMPERIAL) ? Constants.METRIC : Constants.IMPERIAL;

                SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME,0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt("UNITS_OF_MEASURE",_units);
                editor.commit();

                setPebbleUnits();

                if(checkServiceRunning()) {
                    GPSService.setConversionUnits(_units);
                } // reset GPS with new units

            }
        });

        _startButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (!checkServiceRunning()) {
                    startGPSService();
                    _startButton.setText("Stop");
                }
                else{
                    stopGPSService();
                    _startButton.setText("Start");
                }
            }
        });


        _watchfaceButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("http://www.demo.gs/pebblebike.pbw");
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            }
        });

        _pebbleDataHandler = new PebbleKit.PebbleDataReceiver(Constants.WATCH_UUID) {
            @Override
            public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
                int newState = data.getUnsignedInteger(Constants.STATE_CHANGED).intValue();
                int state = newState;
                Log.d("MainActivity","Got Data from Pebble: "  + state);
                PebbleKit.sendAckToPebble(context, transactionId);

                switch(state) {
                    case Constants.STOP_PRESS:
                        stopGPSService();
                        break;
                    case Constants.PLAY_PRESS:
                        startGPSService();
                        break;
                    case Constants.REFRESH_PRESS:
                        break;
                }

                SetupButtons();

            }
        };
        PebbleKit.registerReceivedDataHandler(this, _pebbleDataHandler);

        SetupButtons();

    }

    private void setPebbleUnits() {
        PebbleDictionary dic = new PebbleDictionary();
        dic.addInt32(Constants.MEASUREMENT_UNITS, _units);
        PebbleKit.sendDataToPebble(getApplicationContext(), Constants.WATCH_UUID, dic);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SetupButtons();
    }

    private void SetupButtons() {
        Button _startButton = (Button)findViewById(R.id.MAIN_START_BUTTON);
        ToggleButton _autoStart = (ToggleButton)findViewById(R.id.MAIN_AUTO_START_BUTTON);
        ToggleButton _unitsButton = (ToggleButton)findViewById(R.id.MAIN_UNITS_BUTTON);

        if (checkServiceRunning()) {
            _startButton.setText("Stop");
        }
        else{
            _startButton.setText("Start");
        }

        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME,0);

        _activityRecognition = settings.getBoolean("ACTIVITY_RECOGNITION",false);

        if (_activityRecognition) {
            _startButton.setVisibility(View.GONE);
        }else {
            _startButton.setVisibility(View.VISIBLE);
        }

        _autoStart.setChecked(_activityRecognition);

        if(_activityRecognition && (_mActivityRecognitionClient == null))
            initActivityRecognitionClient();

        _units = settings.getInt("UNITS_OF_MEASURE",Constants.IMPERIAL);
        _unitsButton.setChecked(_units == Constants.IMPERIAL);

        setPebbleUnits();
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
        //set the units
        Intent intent = new Intent(getApplicationContext(), GPSService.class);
        intent.putExtra("UNITS",_units);

        registerGPSServiceIntentReceiver();
        startService(intent);
        setPebbleUnits();
    }

    private void stopGPSService() {
        if(!checkServiceRunning())
            return;
        removeGPSServiceIntentReceiver();
        stopService(new Intent(getApplicationContext(), GPSService.class));
    }

    private void registerGPSServiceIntentReceiver() {
        IntentFilter filter = new IntentFilter(GPSServiceReceiver.ACTION_RESP);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        _gpsServiceReceiver = new GPSServiceReceiver();
        registerReceiver(_gpsServiceReceiver, filter);
    }

    private void removeGPSServiceIntentReceiver() {
        if(_gpsServiceReceiver != null)
            unregisterReceiver(_gpsServiceReceiver);
    }

    private void checkRunkeeperRunning() {
        //check to see if run keeper is running
        ActivityManager activityManager = (ActivityManager) this.getSystemService( ACTIVITY_SERVICE );
        List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
        for(int i = 0; i < procInfos.size(); i++){
            if(procInfos.get(i).processName.equals("com.android.browser")) { //TODO: add runkeeper uri
                Toast.makeText(getApplicationContext(), "Runkeeper is running", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void checkGooglePlayServices() {
        // check to see that google play services are installed and up to date
        int googlePlayServicesAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(googlePlayServicesAvailable !=  ConnectionResult.SUCCESS) {
            // google play services need to be updated
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(googlePlayServicesAvailable, this, 123);
            if(errorDialog != null)
                errorDialog.show();
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

    private void updateActivityType(int type) {
        TextView view = (TextView)this.findViewById(R.id.MAIN_ACTIVITY_TYPE);

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

        view.setText("Activity Type: " + activityType);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if(_requestType == RequestType.START) {
            Log.d("MainActivity","Start Recognition");
            _mActivityRecognitionClient.requestActivityUpdates(30000, _callbackIntent);
        } else {
            Log.d("MainActivity","Stop Recognition");
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

            if(activity == DetectedActivity.ON_BICYCLE)
                startGPSService();
            else
                stopGPSService();

        }
    }

    public class GPSServiceReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP =
                "com.njackson.intent.action.UPDATE_PEBBLE";
        @Override
        public void onReceive(Context context, Intent intent) {

            double speed = intent.getDoubleExtra("SPEED", 99);
            double distance = intent.getDoubleExtra("DISTANCE", 99);
            double avgspeed = intent.getDoubleExtra("AVGSPEED", 99);

            Log.d("MainActivity","Sending Data:" + speed + " dist: " + distance + " avgspeed"  + avgspeed);

            DecimalFormat df = new DecimalFormat("#.#");
            PebbleDictionary dic = new PebbleDictionary();
            dic.addString(Constants.SPEED_TEXT,df.format(speed));
            dic.addString(Constants.DISTANCE_TEXT,df.format(distance));
            dic.addString(Constants.AVGSPEED_TEXT,df.format(avgspeed));
            dic.addInt32(Constants.MEASUREMENT_UNITS, _units);

            if(checkServiceRunning()) {
                dic.addInt32(Constants.STATE_CHANGED,Constants.STATE_START);
            } else {
                dic.addInt32(Constants.STATE_CHANGED,Constants.STATE_STOP);
            }

            PebbleKit.sendDataToPebble(getApplicationContext(), Constants.WATCH_UUID, dic);

        }
    }
}
