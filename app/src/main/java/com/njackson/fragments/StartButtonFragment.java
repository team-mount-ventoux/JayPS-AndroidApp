package com.njackson.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.njackson.R;
import com.njackson.events.GPSServiceCommand.GPSStatus;
import com.njackson.events.UI.StartButtonTouchedEvent;
import com.njackson.events.UI.StopButtonTouchedEvent;
import com.njackson.events.base.BaseStatus;
import com.njackson.utils.services.IServiceStarter;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

/**
 * Created by server on 11/04/2014.
 */
public class StartButtonFragment extends BaseFragment {

    private static final String TAG = "PB-StartButtonFragment";

    private View _view;

    @Inject IServiceStarter _serviceStarter;

    @Subscribe
    public void onGPSServiceState(GPSStatus event) {
        final Button startButton = (Button) _view.findViewById(R.id.start_button);
        if (event.getStatus() == BaseStatus.Status.STOPPED) {
            makeStartButtonInStartState(startButton);
        } else if (event.getStatus() == BaseStatus.Status.STARTED) {
            makeStartButtonInStopState(startButton);
        }
    }

    public StartButtonFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreateView(inflater,container,savedInstanceState);
        _view = inflater.inflate(R.layout.fragment_start_button, container, false);

        SetupOnClick(_view);

        return _view;
    }

    @Override
    public void onResume() {
        super.onResume();
        _serviceStarter.broadcastLocationState();
    }

    private void SetupOnClick(View view) {
        final Button startButton = (Button) view.findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(startButton.getText() == getString(R.string.startbuttonfragment_start)) {
                    _bus.post(new StartButtonTouchedEvent());
                } else {
                    _bus.post(new StopButtonTouchedEvent());
                }
            }
        });
    }

    private void makeStartButtonInStartState(Button startButton) {
        startButton.setText(getString(R.string.startbuttonfragment_start));
        startButton.setBackgroundColor(getResources().getColor(R.color.startbuttonfragment_button_start));
    }
    private void makeStartButtonInStopState(Button startButton) {
        startButton.setText(getString(R.string.startbuttonfragment_stop));
        startButton.setBackgroundColor(getResources().getColor(R.color.startbuttonfragment_button_stop));
    }
}
