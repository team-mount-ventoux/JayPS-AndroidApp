package com.njackson.fragments;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.njackson.R;

/**
 * Created by server on 11/04/2014.
 */
public class StartButtonFragment extends BaseFragment {

    public StartButtonFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        //super.onCreateView(inflater,container,savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_start_button, container, false);
        return view;
    }

}
