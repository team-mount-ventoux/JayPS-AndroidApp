package com.njackson;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragment;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.njackson.util.AltitudeGraphReduce;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: njackson
 * Date: 05/06/2013
 * Time: 21:47
 * To change this template use File | Settings | File Templates.
 */
public class HomeActivity extends SherlockFragment {
	
	private static final String TAG = "PB-HomeActivity";

    OnButtonPressListener _callback;
    // Container Activity must implement this interface
    public interface OnButtonPressListener {
        public void onPressed(int sender, boolean value);
    }

    private RelativeLayout _view;

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        try {
            _callback = (OnButtonPressListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnButtonPressListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            // During initial setup, plug in the details fragment.
            AltitudeFragment alt = new AltitudeFragment();
            getFragmentManager().beginTransaction().add(R.id.MAIN_ALTITUDE, alt,"altitude_fragment").commit();
        } else {
            AltitudeFragment alt = (AltitudeFragment)getFragmentManager().findFragmentByTag("altitude_fragment");
            getFragmentManager().beginTransaction().replace(R.id.MAIN_ALTITUDE,alt).commit();
        }

    }

    @Override
    public void onSaveInstanceState(Bundle instanceState) {

        TextView timeView = (TextView)_view.findViewById(R.id.MAIN_TIME_TEXT);
        instanceState.putString("Time", timeView.getText().toString());

        TextView avgSpeedView = (TextView)_view.findViewById(R.id.MAIN_AVG_SPEED_TEXT);
        instanceState.putString("AvgSpeed",avgSpeedView.getText().toString());

        TextView speedView = (TextView)_view.findViewById(R.id.MAIN_SPEED_TEXT);
        instanceState.putString("Speed",speedView.getText().toString());

        TextView distanceView = (TextView)_view.findViewById(R.id.MAIN_DISTANCE_TEXT);
        instanceState.putString("Distance",distanceView.getText().toString());

        ArrayList<Integer> graphData = AltitudeGraphReduce.getInstance().getCache();
        instanceState.putIntegerArrayList("Altitude", graphData);

        Toast.makeText(getActivity(),"onSave",1);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        _view = (RelativeLayout)inflater.inflate(R.layout.home, container, false);

        if(savedInstanceState != null){

            TextView timeView = (TextView)_view.findViewById(R.id.MAIN_TIME_TEXT);
            timeView.setText(savedInstanceState.getString("Time"));

            TextView avgSpeedView = (TextView)_view.findViewById(R.id.MAIN_AVG_SPEED_TEXT);
            avgSpeedView.setText(savedInstanceState.getString("AvgSpeed"));

            TextView speedView = (TextView)_view.findViewById(R.id.MAIN_SPEED_TEXT);
            speedView.setText(savedInstanceState.getString("Speed"));

            TextView distanceView = (TextView)_view.findViewById(R.id.MAIN_DISTANCE_TEXT);
            distanceView.setText(savedInstanceState.getString("Distance"));

            ArrayList<Integer> graphData = savedInstanceState.getIntegerArrayList("Altitude");
            AltitudeGraphReduce.getInstance().setCache(graphData);
            setAltitude(
                    AltitudeGraphReduce.getInstance().getGraphData(),
                    AltitudeGraphReduce.getInstance().getMax(),
                    AltitudeGraphReduce.getInstance().getMin()
            );

        }

        final Button _startButton = (Button)_view.findViewById(R.id.MAIN_START_BUTTON);
        final ImageView _settingsButton = (ImageView)_view.findViewById(R.id.MAIN_SETTINGS_IMAGE);
        final Context context = this.getActivity();

        _settingsButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                context.startActivity(new Intent(context,SettingsActivity.class));
            }

        });

        _startButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                _callback.onPressed(R.id.MAIN_START_BUTTON,_startButton.getText().equals("Start"));
                if(_startButton.getText().equals("Start")) {
                	setStartButtonText("Stop");
                } else {
                	setStartButtonText("Start");
                }

            }
        });

        // if we are using activity recognition hide the start button
        setStartButtonVisibility(!MainActivity.getInstance().activityRecognitionEnabled());

        if(MainActivity.getInstance().checkServiceRunning())
            setStartButtonText(getString(R.string.START_BUTTON_STOP));
        else
            setStartButtonText(getString(R.string.START_BUTTON_START));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        int units = Integer.valueOf(prefs.getString("UNITS_OF_MEASURE", "0"));
        setUnits(units);

        Toast.makeText(getActivity(),"onCreateView",1);

        return _view;
    }

    public void setStartButtonVisibility(Boolean shown) {
        if (_view == null) {
            Log.e(TAG, "setStartButtonVisibility: _view == null : exit");
            // BUG! This could happen if the system is low on memory and choose to kill the app.
            // All views are not correctly reconstructed when the app restarts, causing the present bug
            // Waiting for a fix for this bug. In the meantime, force the app to close to avoid the "NullPointerException" crash
            // Note: when GPS is running, it is not likely to happen: it runs in a foreground service with a sticky notification for the user
            System.exit(0);
        }
        Button startButton = (Button)_view.findViewById(R.id.MAIN_START_BUTTON);
        if(shown)
            startButton.setVisibility(View.VISIBLE);
        else
            startButton.setVisibility(View.GONE);
    }

    public void setActivityText(String activity) {
        //TextView textView = (TextView)_view.findViewById(R.id.MAIN_ACTIVITY_TYPE);
        //textView.setText("Activity: " + activity);
    }

    public void setStartButtonText(String text) {
        Button button = (Button)_view.findViewById(R.id.MAIN_START_BUTTON);
        button.setText(text);
        if (text.equals("Start")) {
        	button.setBackgroundColor(getResources().getColor(R.color.START_START));
        } else {
        	button.setBackgroundColor(getResources().getColor(R.color.START_STOP));
        }        
    }

    public void setSpeed(String text) {
        TextView textView = (TextView)_view.findViewById(R.id.MAIN_SPEED_TEXT);
        textView.setText(text);
    }

    public void setAvgSpeed(String text) {
        TextView textView = (TextView)_view.findViewById(R.id.MAIN_AVG_SPEED_TEXT);
        textView.setText(text);
    }

    public void setDistance(String text) {
        TextView textView = (TextView)_view.findViewById(R.id.MAIN_DISTANCE_TEXT);
        textView.setText(text);
    }

    public void setTime(String text) {
        TextView textView  = (TextView)_view.findViewById(R.id.MAIN_TIME_TEXT);
        textView.setText(text);
    }

    public void setAltitude(int[] altitude, int maxAltitude, int minAltitude) {
        AltitudeFragment altitudeFragment = (AltitudeFragment)getFragmentManager().findFragmentByTag("altitude_fragment");
        altitudeFragment.setAltitude(altitude,maxAltitude,true);
    }

    // Sets the display units based upon user preference
    public void setUnits(int units) {

        TextView avgSpeedView = (TextView)_view.findViewById(R.id.MAIN_AVG_SPEED_LABEL);

        if(units == Constants.IMPERIAL) {
            avgSpeedView.setText(R.string.AVG_SPEED_TEXT_IMPERIAL);
        } else {
            avgSpeedView.setText(R.string.AVG_SPEED_TEXT_METRIC);
        }

    }

}