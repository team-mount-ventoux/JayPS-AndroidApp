package com.pebblebike.fragments;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.pebblebike.R;
import com.pebblebike.application.PebbleBikeApplication;
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
