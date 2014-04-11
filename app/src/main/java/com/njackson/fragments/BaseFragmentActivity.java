package com.njackson.fragments;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.njackson.application.PebbleBikeApplication;
import com.squareup.otto.Bus;

import javax.inject.Inject;

/**
 * Created by server on 11/04/2014.
 */
public class BaseFragmentActivity extends FragmentActivity {

    @Inject Bus _bus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupInjection();
        _bus.register(this);
    }

    protected void setupInjection() {
        ((PebbleBikeApplication) getApplication()).inject(this);
    }

}
