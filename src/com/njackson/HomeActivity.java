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

                _callback.onPressed(R.id.MAIN_START_BUTTON,_startButton.getText().equals("Start"));
                if(_startButton.getText().equals("Start")) {
                    _startButton.setText("Stop");
                    _startButton.setBackgroundColor(R.color.START_STOP);
                } else {
                    _startButton.setText("Start");
                    _startButton.setBackgroundColor(R.color.START_START);
                }
            }
        });

        /*
        final ToggleButton _autoStart = (ToggleButton)_view.findViewById(R.id.MAIN_AUTO_START_BUTTON);

        final Button _watchfaceButton = (Button)_view.findViewById(R.id.MAIN_INSTALL_WATCHFACE_BUTTON);
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
        int units = args.getInt("UNITS_OF_MEASURE",0);
*/
        //SetupButtons(activityRecognition,units);

        return _view;
    }

    public void SetupButtons(boolean activityRecognition, int units) {

        Button _startButton = (Button)_view.findViewById(R.id.MAIN_START_BUTTON);
        ToggleButton _autoStart = (ToggleButton)_view.findViewById(R.id.MAIN_AUTO_START_BUTTON);
        ToggleButton _unitsButton = (ToggleButton)_view.findViewById(R.id.MAIN_UNITS_BUTTON);

        if (activityRecognition) {
            _startButton.setVisibility(View.GONE);
        }else {
            _startButton.setVisibility(View.VISIBLE);
        }

        _autoStart.setChecked(activityRecognition);

        _unitsButton.setChecked(units == Constants.IMPERIAL);

    }

    public void SetActivityText(String activity) {
        TextView textView = (TextView)_view.findViewById(R.id.MAIN_ACTIVITY_TYPE);
        textView.setText("Activity: " + activity);
    }

    public void SetStartText(String text) {
        Button button = (Button)_view.findViewById(R.id.MAIN_START_BUTTON);
        button.setText(text);
    }

}