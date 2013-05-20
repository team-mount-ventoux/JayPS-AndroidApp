package com.android.AdventureTracker;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.ActivityRecognitionClient;

import java.util.List;

public class MainActivity extends Activity implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener {

    private ActivityRecognitionClient mActivityRecognitionClient;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkGooglePlayServices();
        checkRunkeeperRunning();
        initActivityRecognitionClient();

        setContentView(R.layout.main);
    }

    private void initActivityRecognitionClient() {
        // Connect to the ActivityRecognitionService
        mActivityRecognitionClient = new ActivityRecognitionClient(this, this, this);
        mActivityRecognitionClient.connect();
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

    @Override
    public void onConnected(Bundle connectionHint) {
        Intent intent = new Intent(this, ActivityRecognitionIntentService.class);
        PendingIntent callbackIntent = PendingIntent.getService(this, 0, intent,
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
}
