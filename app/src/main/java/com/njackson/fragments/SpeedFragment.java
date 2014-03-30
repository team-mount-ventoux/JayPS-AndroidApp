package com.njackson.fragments;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.njackson.R;
import com.njackson.application.PebbleBikeApplication;
import com.squareup.otto.Bus;

import javax.inject.Inject;

public class SpeedFragment extends FragmentActivity {

    @Inject Bus _bus;

    public SpeedFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((PebbleBikeApplication) getApplication()).inject(this);

        setContentView(R.layout.fragment_speed);
    }

}
