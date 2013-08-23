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

import java.text.DecimalFormat;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        _view = (RelativeLayout)inflater.inflate(R.layout.home, container, false);
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

                // test animation
                //int[] alts = new int[] {100,200,200,200,300,400,500,600,700,800,900,1000,1000,1000};
                //AltitudeFragment altitudeFragment = (AltitudeFragment)getFragmentManager().findFragmentByTag("tag_fragment_altitudefragment");
                //altitudeFragment.setAltitude(alts,1000,true);

                _callback.onPressed(R.id.MAIN_START_BUTTON,_startButton.getText().equals("Start"));
                if(_startButton.getText().equals("Start")) {
                	setStartButtonText("Stop");
                } else {
                	setStartButtonText("Start");
                }

            }
        });

        /*
        final ToggleButton _autoStart = (ToggleButton)_view.findViewById(R.id.MAIN_AUTO_START_BUTTON);

        final Button _watchfaceButton = (Button)_view.findViewById(R.id.MAIN_INSTALL_WATCHFACE_BUTTON);
        final ToggleButton _liveTrackingButton = (ToggleButton)_view.findViewById(R.id.MAIN_LIVE_TRACKING_BUTTON);
        final ToggleButton _unitsButton = (ToggleButton)_view.findViewById(R.id.MAIN_UNITS_BUTTON);

        //_autoStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                _callback.onPressed(R.id.MAIN_AUTO_START_BUTTON,_autoStart.isChecked());
                getArguments().putBoolean("ACTIVITY_RECOGNITION",_autoStart.isChecked());
                if(_autoStart.isChecked())
                    _startButton.setVisibility(View.GONE);
                else
                    _startButton.setVisibility(View.VISIBLE);
            }
        });
        
        //_liveTrackingButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
            	_callback.onPressed(R.id.MAIN_LIVE_TRACKING_BUTTON,_liveTrackingButton.isChecked());
                getArguments().putBoolean("LIVE_TRACKING",_liveTrackingButton.isChecked());
            }
        });

        //_unitsButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                getArguments().putInt("UNITS_OF_MEASURE",(_autoStart.isChecked()) ? Constants.IMPERIAL : Constants.METRIC);
                _callback.onPressed(R.id.MAIN_UNITS_BUTTON,_unitsButton.isChecked());

            }
        });
       */

        /*
        _watchfaceButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                 _callback.onPressed(R.id.MAIN_INSTALL_WATCHFACE_BUTTON,true);
            }
        });

        Bundle args = getArguments();
        boolean activityRecognition = args.getBoolean("ACTIVITY_RECOGNITION",false);
        boolean liveTracking = args.getBoolean("LIVE_TRACKING",false);
        int units = args.getInt("UNITS_OF_MEASURE",0);
*/
        //SetupButtons(activityRecognition,liveTracking,units);

        return _view;
    }

    /*public void SetupButtons(boolean activityRecognition, boolean liveTracking, int units) {

        Button _startButton = (Button)_view.findViewById(R.id.MAIN_START_BUTTON);
        ToggleButton _autoStart = (ToggleButton)_view.findViewById(R.id.MAIN_AUTO_START_BUTTON);
        ToggleButton _liveTrackingButton = (ToggleButton)_view.findViewById(R.id.MAIN_LIVE_TRACKING_BUTTON);
        ToggleButton _unitsButton = (ToggleButton)_view.findViewById(R.id.MAIN_UNITS_BUTTON);

        if (activityRecognition) {
            _startButton.setVisibility(View.GONE);
        }else {
            _startButton.setVisibility(View.VISIBLE);
        }

        _autoStart.setChecked(activityRecognition);

        _liveTrackingButton.setChecked(liveTracking);
        _unitsButton.setChecked(units == Constants.IMPERIAL);

    }*/

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

}