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
import com.njackson.events.ActivityRecognitionCommand.ActivityRecognitionStatus;
import com.njackson.events.UI.StartButtonTouchedEvent;
import com.njackson.events.UI.StopButtonTouchedEvent;
import com.njackson.events.GoogleFitCommand.GoogleFitStatus;
import com.njackson.utils.googleplay.IGooglePlayServices;
import com.njackson.utils.services.IServiceStarter;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

import de.cketti.library.changelog.ChangeLog;

public class MainActivity extends FragmentActivity  implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "PB-MainActivity";
    @Inject Bus _bus;
    @Inject IAnalytics _analytics;
    @Inject IServiceStarter _serviceStarter;
    @Inject IGooglePlayServices _playServices;
    @Inject SharedPreferences _sharedPreferences;
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
    public void onRecognitionState(ActivityRecognitionStatus event) {
        if(event.getState().compareTo(ActivityRecognitionStatus.State.PLAY_SERVICES_NOT_AVAILABLE) == 0)
            Log.d(TAG, "PLAY_NOT_AVIALABLE");
    }

    @Subscribe
    public void onGoogleFitStatusChanged(GoogleFitStatus event) {
        if(event.getState() == GoogleFitStatus.State.GOOGLEFIT_CONNECTION_FAILED) {
            if(!_playServices.connectionResultHasResolution(event.getConnectionResult())) {
                _playServices.showConnectionResultErrorDialog(event.getConnectionResult(), this);
                return;
            }

            handleGoogleFitFailure(event);
        }
    }

    private void handleGoogleFitFailure(GoogleFitStatus event) {
        if (!_authInProgress) {
            try {
                Log.i(TAG, "Attempting to resolve failed connection");
                _authInProgress = true;
                _playServices.startConnectionResultResolution(event.getConnectionResult(),this);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG,"Exception while starting resolution activity", e);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((PebbleBikeApplication) getApplication()).inject(this);

        _analytics.trackAppOpened(getIntent());
        _serviceStarter.startMainService();

        boolean activity_start = _sharedPreferences.getBoolean("ACTIVITY_RECOGNITION",false);
        boolean fit_start = _sharedPreferences.getBoolean("GOOGLE_FIT",false);
        if(fit_start || activity_start) {
            _serviceStarter.startActivityServices();
        }

        ChangeLog cl = new ChangeLog(this);
        if (cl.isFirstRun()) {
            cl.getLogDialog().show();
        }

        if (getIntent().getExtras() != null) {
            onNewIntent(getIntent());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        _bus.register(this);
        _sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        _bus.unregister(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        _sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.compareTo("ACTIVITY_RECOGNITION") == 0 || key.compareTo("GOOGLE_FIT") == 0) {
            boolean activity_start = sharedPreferences.getBoolean("ACTIVITY_RECOGNITION",false);
            boolean fit_start = sharedPreferences.getBoolean("GOOGLE_FIT",false);
            if(activity_start || fit_start) {
                _serviceStarter.startActivityServices();
            } else {
                _serviceStarter.stopActivityServices();
            }
        }
    }
}
