package com.njackson.fragments;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.njackson.R;
import com.njackson.events.GPSService.NewLocationEvent;
import com.squareup.otto.Subscribe;

public class SpeedFragment extends BaseFragment {

    public SpeedFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreateView(inflater,container,savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_speed, container, false);
        return view;
    }

    @Subscribe
    public void onNewLocation(NewLocationEvent event) {
        TextView speedText = (TextView)getActivity().findViewById(R.id.speed_text);
        speedText.setText(Float.toString(event.getSpeed()));

        TextView avgSpeed = (TextView)getActivity().findViewById(R.id.avgspeed_text);
        avgSpeed.setText(Float.toString(event.getAvgSpeed()));

        TextView distance = (TextView)getActivity().findViewById(R.id.distance_text);
        distance.setText(Float.toString(event.getDistance()));

        TextView time = (TextView)getActivity().findViewById(R.id.time_text);
        String timeText = DateUtils.formatElapsedTime(event.getElapsedTimeSeconds());
        time.setText(timeText);
    }

}
