package com.njackson.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.njackson.R;
import com.njackson.events.GPSServiceCommand.NewLocation;
import com.njackson.utils.NumberConverter;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

public class SpeedFragment extends BaseFragment {

    private String TAG = "PB-SpeedFragment";

    @Inject SharedPreferences _sharedPreferences;

    private boolean _restoreInstanceState;


    @Subscribe
    public void onNewLocation(NewLocation event) {
        NumberConverter converter = new NumberConverter();

        TextView speedText = (TextView)getActivity().findViewById(R.id.speed_text);
        speedText.setText(converter.converFloatToString(event.getSpeed(),1));

        TextView avgSpeed = (TextView)getActivity().findViewById(R.id.avgspeed_text);
        avgSpeed.setText(converter.converFloatToString(event.getAverageSpeed(),1));

        TextView distance = (TextView)getActivity().findViewById(R.id.distance_text);
        distance.setText(converter.converFloatToString(event.getDistance(),1));

        TextView time = (TextView)getActivity().findViewById(R.id.time_text);
        String timeText = DateUtils.formatElapsedTime(event.getElapsedTimeSeconds());
        time.setText(timeText);
    }

    public SpeedFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_speed, container, false);

		_restoreInstanceState = false;
		if (savedInstanceState != null) {
            _restoreInstanceState = true;
        }

        return view;
    }

    @Override
    public void onResume() {
        //Log.d(TAG, "onResume");
        super.onResume();

        restoreFromPreferences();
    }

    @Override
    public void onPause() {
        //Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onDestroy() {
        //Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //Log.d(TAG, "onSaveInstanceState");

        SharedPreferences.Editor editor = _sharedPreferences.edit();

        TextView speedText = (TextView)getActivity().findViewById(R.id.speed_text);
        editor.putString("SPEEDFRAGMENT_SPEED",speedText.getText().toString());

        TextView avgSpeed = (TextView)getActivity().findViewById(R.id.avgspeed_text);
        editor.putString("SPEEDFRAGMENT_AVGSPEED",avgSpeed.getText().toString());

        TextView distance = (TextView)getActivity().findViewById(R.id.distance_text);
        editor.putString("SPEEDFRAGMENT_DISTANCE",distance.getText().toString());

        TextView time = (TextView)getActivity().findViewById(R.id.time_text);
        editor.putString("SPEEDFRAGMENT_TIME",time.getText().toString());

        editor.commit();

        super.onSaveInstanceState(outState);
    }

    private void restoreFromPreferences() {
        //Log.d(TAG, "restoreFromPreferences");
        TextView speedText = (TextView)getActivity().findViewById(R.id.speed_text);
        if (_restoreInstanceState) {
            speedText.setText(_sharedPreferences.getString("SPEEDFRAGMENT_SPEED", getString(R.string.speedfragment_speed_value)));
        } else {
            // start of the app, force instant speed to 0
            speedText.setText(getString(R.string.speedfragment_speed_value));
        }

        TextView avgSpeed = (TextView)getActivity().findViewById(R.id.avgspeed_text);
        avgSpeed.setText(_sharedPreferences.getString("SPEEDFRAGMENT_AVGSPEED", getString(R.string.speedfragment_avgspeed_value)));

        TextView distance = (TextView)getActivity().findViewById(R.id.distance_text);
        distance.setText(_sharedPreferences.getString("SPEEDFRAGMENT_DISTANCE", getString(R.string.speedfragment_distance_value)));

        TextView time = (TextView)getActivity().findViewById(R.id.time_text);
        time.setText(_sharedPreferences.getString("SPEEDFRAGMENT_TIME", getString(R.string.speedfragment_time_value)));
    }
}
