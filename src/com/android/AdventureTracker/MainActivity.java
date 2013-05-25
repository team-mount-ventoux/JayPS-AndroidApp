package com.android.AdventureTracker;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.*;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.internal.r;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;

import java.util.List;

public class MainActivity extends Activity implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener {

    private ActivityRecognitionClient mActivityRecognitionClient;
    private ResponseReceiver receiver;
    private boolean _gpsRunning = false;
    private boolean _activityRecognition = false;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkGooglePlayServices();
        checkRunkeeperRunning();
        registerIntentReceiver();
        setContentView(R.layout.main);

        final ToggleButton _autoStart = (ToggleButton)findViewById(R.id.autoStartButton);
        final Button _startButton = (Button)findViewById(R.id.startButton);

        _autoStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                _activityRecognition = !_activityRecognition;
                if (_activityRecognition) {
                    initActivityRecognitionClient();
                    _startButton.setVisibility(0);
                }else {
                    stopActivityRecogntionClient();
                    _startButton.setVisibility(1);
                }
            }
        });

        _startButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                _gpsRunning = !_gpsRunning;
                if (_gpsRunning)
                    startGPSService();
                else
                    stopGPSService();
            }
        });

    }

    private void stopActivityRecogntionClient() {
        mActivityRecognitionClient.disconnect();
    }

    private void initActivityRecognitionClient() {
        // Connect to the ActivityRecognitionService
        mActivityRecognitionClient = new ActivityRecognitionClient(getApplicationContext(), this, this);
        mActivityRecognitionClient.connect();
    }

    private void registerIntentReceiver() {
        IntentFilter filter = new IntentFilter(ResponseReceiver.ACTION_RESP);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new ResponseReceiver();
        registerReceiver(receiver, filter);
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

    private void updateActivityType(String type) {
        TextView view = (TextView)this.findViewById(R.id.activityType);
        view.setText("Activity Type: " + type);
    }

    private void startGPSService() {
        if(_gpsRunning)
           return;

        startService(new Intent(getApplicationContext(),GPSService.class));
        _gpsRunning = true;
    }

    private void stopGPSService() {
        if(!_gpsRunning)
            return;
        stopService(new Intent(getApplicationContext(),GPSService.class));
        _gpsRunning = false;
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Intent intent = new Intent(getApplicationContext(), ActivityRecognitionIntentService.class);

        PendingIntent callbackIntent = PendingIntent.getService(getApplicationContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mActivityRecognitionClient.requestActivityUpdates(30000, callbackIntent);
    }

    @Override
    public void onDisconnected() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public class ResponseReceiver extends BroadcastReceiver {
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
}
