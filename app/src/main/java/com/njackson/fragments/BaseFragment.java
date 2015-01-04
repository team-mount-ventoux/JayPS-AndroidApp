package com.njackson.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.njackson.application.PebbleBikeApplication;
import com.squareup.otto.Bus;

import javax.inject.Inject;

/**
 * Created by server on 11/04/2014.
 */
public class BaseFragment extends Fragment {

    @Inject Bus _bus;

    public BaseFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        setupInjection();
        return null;
    }

    @Override
    public void onResume() {
        _bus.register(this);
        super.onPause();
    }

    @Override
    public void onPause() {
        _bus.unregister(this);
        super.onPause();
    }

    protected void setupInjection() {
        ((PebbleBikeApplication) getActivity().getApplication()).inject(this);
    }

}
