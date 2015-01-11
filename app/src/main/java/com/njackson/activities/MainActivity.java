package com.njackson.activities;

import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.njackson.R;
import com.njackson.analytics.IAnalytics;
import com.njackson.application.PebbleBikeApplication;
import com.njackson.application.SettingsActivity;
import com.njackson.events.ActivityRecognitionService.CurrentState;
import com.njackson.events.UI.StartButtonTouchedEvent;
import com.njackson.events.UI.StopButtonTouchedEvent;
import com.njackson.events.status.GoogleFitStatus;
import com.njackson.utils.googleplay.IGooglePlayServices;
import com.njackson.utils.services.IServiceStarter;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

public class MainActivity extends FragmentActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "MainActivity";
    @Inject Bus _bus;
    @Inject IAnalytics _analytics;
    @Inject SharedPreferences _sharedPreferences;
    @Inject IServiceStarter _serviceStarter;
    @Inject IGooglePlayServices _playServices;
    private boolean _authInProgress;

    @Subscribe
    public void onStartButtonTouched(StartButtonTouchedEvent event) {
        _serviceStarter.startLocationServices();
    }

    @Subscribe
    public void onStopButtonTouched(StopButtonTouchedEvent event) {
        _serviceStarter.stopLocationServices();
    }

    @Subscribe
    public void onRecognitionState(CurrentState event) {
        if(event.getState().compareTo(CurrentState.State.PLAY_SERVICES_NOT_AVAILABLE) == 0)
            Log.d(TAG, "PLAY_NOT_AVIALABLE");
        else
            Log.d(TAG, "SERVICE_STARTED");
    }

    @Subscribe
    public void onGoogleFitStatusChanged(GoogleFitStatus event) {
        if(event.getState() == GoogleFitStatus.State.GOOGLEFIT_CONNECTION_FAILED) {
            if(!_playServices.connectionResultHasResolution(event.getConnectionResult())) {
                _playServices.showConnectionResultErrorDialog(event.getConnectionResult(), this);
                return;
            }
        }

        // The failure has a resolution. Resolve it.
        // Called typically when the app is not yet authorized, and an
        // authorization dialog is displayed to the user.
        if (!_authInProgress) {
            try {
                Log.i(TAG, "Attempting to resolve failed connection");
                _authInProgress = true;
                _playServices.startConnectionResultResolution(event.getConnectionResult(),this);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG,
                        "Exception while starting resolution activity", e);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((PebbleBikeApplication) getApplication()).inject(this);

        setContentView(R.layout.activity_main);

        _analytics.trackAppOpened(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();

        _bus.register(this);

        if(_sharedPreferences.getBoolean("ACTIVITY_RECOGNITION",false)) {
            _serviceStarter.startRecognitionServices();
        }

        _sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        _bus.unregister(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        _sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.compareTo("ACTIVITY_RECOGNITION") == 0) {
            boolean start = sharedPreferences.getBoolean("ACTIVITY_RECOGNITION",false);
            if(start) {
                _serviceStarter.startRecognitionServices();
            } else {
                _serviceStarter.stopRecognitionServices();
            }
        }
    }
}
