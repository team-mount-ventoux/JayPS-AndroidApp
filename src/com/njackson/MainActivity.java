package com.njackson;

import android.app.ActivityManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends SherlockFragmentActivity  implements  GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, HomeActivity.OnButtonPressListener {

    private ActivityRecognitionClient _mActivityRecognitionClient;

    private static boolean _activityRecognition = false;
    private PendingIntent _callbackIntent;
    private RequestType _requestType;
    private static int _units = Constants.IMPERIAL;

    private Date _lastCycling;

    private ActivityRecognitionReceiver _activityRecognitionReceiver;
    private GPSServiceReceiver _gpsServiceReceiver;
    private boolean _googlePlayInstalled;

    private boolean _sending = false;
    private byte[] _data;
    private int _progress=0;
    private int _chunksize=100;

    enum RequestType {
        START,
        STOP
    }

    // Listener for the fragment button press
    @Override
    public void onPressed(int sender, boolean value) {
        //To change body of implemented methods use File | Settings | File Templates.
        switch(sender) {
            case R.id.MAIN_AUTO_START_BUTTON:
                autoStartButtonClick(value);
                break;
            case R.id.MAIN_START_BUTTON:
                startButtonClick(value);
                break;
            case R.id.MAIN_UNITS_BUTTON:
                unitsButtonClick(value);
                break;
            case R.id.MAIN_INSTALL_WATCHFACE_BUTTON:
                sendWatchFaceToPebble();
                break;
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

    private void startButtonClick(boolean value) {



        if(value) {
            try {
                SendImageToPebble();
            } catch (FileNotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            startGPSService();
        } else {
            stopGPSService();
        }
    }

    private void unitsButtonClick(boolean value) {
        if(value) {
            _units = Constants.IMPERIAL;
        } else {
            _units = Constants.METRIC;
        }
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("UNITS_OF_MEASURE",_units);
        editor.commit();
        setPebbleUnits();
        if(checkServiceRunning()) {
            GPSService.setConversionUnits(_units);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayHomeAsUpEnabled(false);

        checkGooglePlayServices();

        //setup the defaults
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME,0);
        _activityRecognition = settings.getBoolean("ACTIVITY_RECOGNITION",false);
        _units = settings.getInt("UNITS_OF_MEASURE",0);

        Bundle bundle = new Bundle();
        bundle.putBoolean("ACTIVITY_RECOGNITION",_activityRecognition);
        bundle.putInt("UNITS_OF_MEASURE",_units);

        actionBar.addTab(actionBar.newTab().setText(R.string.TAB_TITLE_HOME).setTabListener(new TabListener<HomeActivity>(this, "home", HomeActivity.class, bundle)));
        //actionBar.addTab(actionBar.newTab().setText(R.string.TAB_TITLE_HOME).setTabListener(new TabListener<MapActivity>(this, "map", MapActivity.class, bundle)));

        setupPebbleButtonHandler();

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void sendWatchFaceToPebble(){
        try {
            Uri uri = Uri.parse("http://www.demo.gs/pebblebike.pbw");
            Intent startupIntent = new Intent();
            startupIntent.setAction(Intent.ACTION_VIEW);
            startupIntent.setType("application/octet-stream");
            startupIntent.setData(uri);
            ComponentName distantActivity = new ComponentName("com.getpebble.android", "com.getpebble.android.ui.UpdateActivity");
            startupIntent.setComponent(distantActivity);
            startActivity(startupIntent);
        }catch (ActivityNotFoundException ae) {
            Toast.makeText(getApplicationContext(),"Unable to install watchface, do you have the latest pebble app installed?",10);
        }
    }

    private void setPebbleUnits() {
        PebbleDictionary dic = new PebbleDictionary();
        dic.addInt32(Constants.MEASUREMENT_UNITS, _units);
        PebbleKit.sendDataToPebble(getApplicationContext(), Constants.WATCH_UUID, dic);
    }

    private void sendServiceState() {
        PebbleDictionary dic = new PebbleDictionary();
        if(checkServiceRunning()) {
            dic.addInt32(Constants.STATE_CHANGED,Constants.STATE_START);
        } else {
            dic.addInt32(Constants.STATE_CHANGED,Constants.STATE_STOP);
        }
        PebbleKit.sendDataToPebble(getApplicationContext(), Constants.WATCH_UUID, dic);
    }

    private void setupPebbleButtonHandler() {
        PebbleKit.PebbleDataReceiver _pebbleDataHandler = new PebbleKit.PebbleDataReceiver(Constants.WATCH_UUID) {
            @Override
            public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
                int newState = data.getUnsignedInteger(Constants.STATE_CHANGED).intValue();
                int state = newState;
                Log.d("MainActivity", "Got Data from Pebble: " + state);
                PebbleKit.sendAckToPebble(context, transactionId);

                switch (state) {
                    case Constants.STOP_PRESS:
                        stopGPSService();
                        SetStartButtonText("Start");
                        break;
                    case Constants.PLAY_PRESS:
                        startGPSService();
                        SetStartButtonText("Stop");
                        break;
                    case Constants.REFRESH_PRESS:
                        ResetSavedGPSStats();
                        break;
                    case Constants.ACK:
                        SendNextDataBundle();
                        break;
                }

                //SetupButtons();

            }
        };

        PebbleKit.registerReceivedDataHandler(this, _pebbleDataHandler);
    }

    private void SendImageToPebble() throws FileNotFoundException {

        String path =  android.os.Environment.getExternalStorageDirectory().getAbsoluteFile() + "/downloadedFile.png";
        Bitmap orig = BitmapFactory.decodeFile(path);
        Bitmap croppedBmp = Bitmap.createBitmap(orig, 0, 0, 120, 120);

        ColorMatrix bwMatrix =new ColorMatrix();
        bwMatrix.setSaturation(0);
        final ColorMatrixColorFilter colorFilter= new ColorMatrixColorFilter(bwMatrix);
        Bitmap rBitmap = croppedBmp.copy(Bitmap.Config.ARGB_8888, true);
        Paint paint=new Paint();
        paint.setColorFilter(colorFilter);
        Canvas myCanvas =new Canvas(rBitmap);
        myCanvas.drawBitmap(rBitmap, 0, 0, paint);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        rBitmap.compress(Bitmap.CompressFormat.PNG, 85, stream);

        _data = stream.toByteArray();
        _progress = 0;
        _sending = true;
        SendNextDataBundle();

    }

    private void SendNextDataBundle() {

        int size = 0;
        byte[] buffer;

        if(_progress * _chunksize < _data.length) {
            size = (_data.length - _progress < _chunksize) ? (_data.length - _progress) : 80;

            buffer = new byte[size+1];
            buffer[0] = (byte)_progress;
            int r=1;
            for(int n=_chunksize * _progress;n < size + _progress * _chunksize;n++) {
                buffer[r] = _data[n];
                r++;
            }
        }else {
            buffer = new byte[1];
            buffer[0] = Byte.MAX_VALUE;
        }

        _progress ++;

        PebbleDictionary dic = new PebbleDictionary();
        dic.addBytes(Constants.PEBBLEBIKE_DATA_KEY,buffer);
        PebbleKit.sendDataToPebble(getApplicationContext(), Constants.WATCH_UUID, dic);

    }

    private void ResetSavedGPSStats() {
        GPSService.resetGPSStats();
    }

    private void SetStartButtonText(String text) {
        HomeActivity activity = (HomeActivity)(getSupportFragmentManager().findFragmentByTag("home"));
        if(activity != null)
            activity.SetStartText(text);
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

        if(!_googlePlayInstalled) {
            Toast.makeText(getApplicationContext(),"Please install google play services",10);
            return;
        }

        //set the units
        Intent intent = new Intent(getApplicationContext(), GPSService.class);
        intent.putExtra("UNITS",_units);

        registerGPSServiceIntentReceiver();
        startService(intent);
        setPebbleUnits();
        sendServiceState();
    }

    private void stopGPSService() {
        if(!checkServiceRunning())
            return;
        removeGPSServiceIntentReceiver();
        stopService(new Intent(getApplicationContext(), GPSService.class));
        sendServiceState();
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

        HomeActivity activity = (HomeActivity)(getSupportFragmentManager().findFragmentByTag("home"));
        if(activity != null)
            activity.SetActivityText(activityType);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Intent intent = new Intent(getApplicationContext(), ActivityRecognitionIntentService.class);
        _callbackIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if(_requestType == RequestType.START) {
            Log.d("MainActivity", "Start Recognition");
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

    public void saveMapTile(LatLng location) {
        //;
        try
        {
            String urlString = String.format("http://maps.googleapis.com/maps/api/staticmap?center=%f,%f&zoom=16&size=400x400&sensor=false&key=AIzaSyDnwAQuwn8MV9HT1SY6Py8S3HrKiJyq7g4",location.latitude,location.longitude);

            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);
            urlConnection.connect();

            File SDCardRoot = android.os.Environment.getExternalStorageDirectory().getAbsoluteFile();
            String filename="downloadedFile.png";
            Log.i("Local filename:",filename);
            File file = new File(SDCardRoot,filename);
            file.createNewFile();
            FileOutputStream fileOutput = new FileOutputStream(file);
            InputStream inputStream = urlConnection.getInputStream();
            int totalSize = urlConnection.getContentLength();
            int downloadedSize = 0;
            byte[] buffer = new byte[1024];
            int bufferLength = 0;
            while ( (bufferLength = inputStream.read(buffer)) > 0 )
            {
                fileOutput.write(buffer, 0, bufferLength);
                downloadedSize += bufferLength;
                Log.i("Progress:","downloadedSize:"+downloadedSize+"totalSize:"+ totalSize) ;
            }
            fileOutput.close();
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private class AsyncTaskEx extends AsyncTask<LatLng, Void, Void> {

        /** The system calls this to perform work in a worker thread and
         * delivers it the parameters given to AsyncTask.execute() */
        @Override
        protected Void doInBackground(LatLng... locations) {
            saveMapTile(locations[0]);//call your method here it will run in background
            return null;
        }

        /** The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground() */
        @Override
        protected void onPostExecute(Void result) {
            //Write some code you want to execute on UI after doInBackground() completes
            return ;
        }

        @Override
        protected void onPreExecute() {
            //Write some code you want to execute on UI before doInBackground() starts
            return ;
        }
    }

    public class ActivityRecognitionReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP =
                "com.njackson.intent.action.MESSAGE_PROCESSED";
        @Override
        public void onReceive(Context context, Intent intent) {

            int activity = intent.getIntExtra("ACTIVITY_CHANGED", 0);
            updateActivityType(activity);

            if(activity == DetectedActivity.ON_BICYCLE) {
                Log.d("MainActivity","AutoStart");
                startGPSService();
                _lastCycling = new Date();
            } else {
                Log.d("MainActivity","Waiting for stop");
                // check to see if we have been inactive for 2 minutes
                Calendar cal = Calendar.getInstance();
                cal.setTime(new Date());
                cal.add(Calendar.MINUTE, -2);
                if(_lastCycling == null || _lastCycling.before(cal.getTime())) {
                    Log.d("MainActivity","AutoStop");
                    stopGPSService();
                    _lastCycling = null;
                }
            }

        }
    }

    public class GPSServiceReceiver extends BroadcastReceiver {
        public static final String ACTION_RESP =
                "com.njackson.intent.action.UPDATE_PEBBLE";
        @Override
        public void onReceive(Context context, Intent intent) {

            double speed = intent.getFloatExtra("SPEED", 99);
            double distance = intent.getFloatExtra("DISTANCE", 99);
            double avgspeed = intent.getFloatExtra("AVGSPEED", 99);

            Log.d("MainActivity", "Sending Data:" + speed + " dist: " + distance + " avgspeed" + avgspeed);

            DecimalFormat df = new DecimalFormat("#.#");
            PebbleDictionary dic = new PebbleDictionary();
            /*
            dic.addString(Constants.SPEED_TEXT,df.format(speed));
            dic.addString(Constants.DISTANCE_TEXT,df.format(distance));
            dic.addString(Constants.AVGSPEED_TEXT,df.format(avgspeed));
            dic.addInt32(Constants.MEASUREMENT_UNITS, _units);

            if(checkServiceRunning()) {
                dic.addInt32(Constants.STATE_CHANGED,Constants.STATE_START);
            } else {
                dic.addInt32(Constants.STATE_CHANGED,Constants.STATE_STOP);
            }
            */
            //byte data[] = new byte[78];
            //dic.addBytes(Constants.PEBBLEBIKE_DATA_KEY,data);
            //PebbleKit.sendDataToPebble(getApplicationContext(), Constants.WATCH_UUID, dic);


            // do we need to update the map
            double lat =  intent.getDoubleExtra("LAT",0);
            double lon = intent.getDoubleExtra("LON",0);
            LatLng location = new LatLng(lat,lon);
            //new AsyncTaskEx().execute(location);

            HomeActivity activity = (HomeActivity)getSupportFragmentManager().findFragmentByTag("home");
            if(activity != null) {
                try {
                    activity.setLocation(location);
                }catch (IOException io) {
                    Log.d("MainActivity",io.toString());
                }
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

}