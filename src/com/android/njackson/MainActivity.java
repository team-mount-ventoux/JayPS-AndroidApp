package com.android.njackson;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener {

    private ActivityRecognitionClient _mActivityRecognitionClient;
    private ActivityRecognitionReceiver _activityRecognitionReceiver;
    private GPSServiceReceiver _gpsServiceReceiver;
    private boolean _activityRecognition = false;
    PendingIntent _callbackIntent;
    private RequestType _requestType;


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

        checkGooglePlayServices();
        checkRunkeeperRunning();
        setContentView(R.layout.main);

        Intent intent = new Intent(getApplicationContext(), ActivityRecognitionIntentService.class);

        _callbackIntent = PendingIntent.getService(getApplicationContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        final ToggleButton _autoStart = (ToggleButton)findViewById(R.id.autoStartButton);
        final Button _startButton = (Button)findViewById(R.id.startButton);

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

        SetupButtons();

    }

    @Override
    protected void onResume() {
        super.onResume();
        SetupButtons();
    }

    private void SetupButtons() {
        Button _startButton = (Button)findViewById(R.id.startButton);
        ToggleButton _autoStart = (ToggleButton)findViewById(R.id.autoStartButton);

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

        registerGPSServiceIntentReceiver();
        startService(new Intent(getApplicationContext(), GPSService.class));
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

    private void updateActivityType(String type) {
        TextView view = (TextView)this.findViewById(R.id.activityType);
        view.setText("Activity Type: " + type);
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

            TextView result = (TextView) findViewById(R.id.activityType);
            int activity = intent.getIntExtra("ACTIVITY_CHANGED", 0);
            result.setText("Activity Type: " + activity);

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
            PebbleKit.sendDataToPebble(getApplicationContext(), Constants.WATCH_UUID, dic);

        }
    }
}
