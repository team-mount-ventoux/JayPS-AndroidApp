package com.njackson.fragments;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.TextView;

import com.njackson.R;
import com.njackson.application.IPebbleBikeApplication;
import com.njackson.application.PebbleBikeApplication;
import com.njackson.events.GPSService.NewLocationEvent;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

public class SpeedFragment extends FragmentActivity {

    @Inject Bus _bus;

    public SpeedFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupInjection();
        _bus.register(this);

        setContentView(R.layout.fragment_speed);
    }

    protected void setupInjection() {
        ((PebbleBikeApplication) getApplication()).inject(this);
    }

    @Subscribe
    public void onNewLocation(NewLocationEvent event) {
        TextView speedText = (TextView)findViewById(R.id.speed_text);
        speedText.setText(Float.toString(event.getSpeed()));
    }

}
