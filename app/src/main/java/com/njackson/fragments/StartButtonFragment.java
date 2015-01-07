package com.njackson.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.njackson.R;
import com.njackson.events.UI.StartButtonTouchedEvent;
import com.njackson.events.UI.StopButtonTouchedEvent;

import javax.inject.Inject;

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
        super.onCreateView(inflater,container,savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_start_button, container, false);

        SetupOnClick(view);

        return view;
    }

    private void SetupOnClick(View view) {
        final Button startButton = (Button) view.findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(startButton.getText() == getString(R.string.startbuttonfragment_start)) {
                    _bus.post(new StartButtonTouchedEvent());
                    startButton.setText(getString(R.string.startbuttonfragment_stop));
                    startButton.setBackgroundColor(getResources().getColor(R.color.startbuttonfragment_button_stop));
                } else {
                    _bus.post(new StopButtonTouchedEvent());
                    startButton.setText(getString(R.string.startbuttonfragment_start));
                    startButton.setBackgroundColor(getResources().getColor(R.color.startbuttonfragment_button_start));
                }
            }
        });
    }

}
