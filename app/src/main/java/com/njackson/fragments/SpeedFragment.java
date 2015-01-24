package com.njackson.fragments;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.njackson.R;
import com.njackson.events.GPSServiceCommand.NewLocation;
import com.njackson.utils.NumberConverter;
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

}
