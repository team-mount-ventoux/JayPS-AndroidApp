package com.njackson.activities;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.njackson.Constants;
import com.njackson.R;
import com.njackson.analytics.IAnalytics;
import com.njackson.application.PebbleBikeApplication;
import com.njackson.application.SettingsActivity;
import com.njackson.events.PebbleService.NewMessage;
import com.njackson.events.status.ActivityRecognitionStatus;
import com.njackson.events.UI.StartButtonTouchedEvent;
import com.njackson.events.UI.StopButtonTouchedEvent;
import com.njackson.events.status.GoogleFitStatus;
import com.njackson.utils.googleplay.IGooglePlayServices;
import com.njackson.utils.services.IServiceStarter;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

public class MainActivity extends FragmentActivity {

    private static final String TAG = "PB-MainActivity";
    @Inject Bus _bus;
    @Inject IAnalytics _analytics;
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
    public void onRecognitionState(ActivityRecognitionStatus event) {
        if(event.getState().compareTo(ActivityRecognitionStatus.State.PLAY_SERVICES_NOT_AVAILABLE) == 0)
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

            // The failure has a resolution. Resolve it.
            // Called typically when the app is not yet authorized, and an
            // authorization dialog is displayed to the user.
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
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((PebbleBikeApplication) getApplication()).inject(this);

        _analytics.trackAppOpened(getIntent());
        _serviceStarter.startEssentialServices();

        if (getIntent().getExtras() != null) {
            onNewIntent(getIntent());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        _bus.register(this);
    }

    @Override
    protected void onPause() {
        _bus.unregister(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
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

    // This is called for activities that set launchMode to "singleTop" in their package, or if a client used the FLAG_ACTIVITY_SINGLE_TOP flag when calling startActivity(Intent).
    // In either case, when the activity is re-launched while at the top of the activity stack instead of a new instance of the activity being started, onNewIntent() will be called
    // on the existing instance with the Intent that was used to re-launch it.
    // An activity will always be paused before receiving a new intent, so you can count on onResume() being called after this method.
    // TODO: write tests for intent handler
    protected void onNewIntent (Intent intent) {
        if (intent.getExtras() != null) {
            if (intent.getExtras().containsKey("button")) {
                Log.d(TAG, "onNewIntent() button:" + intent.getExtras().getInt("button"));
                changeState(intent.getExtras().getInt("button"));
            }
            if (intent.getExtras().containsKey("version")) {
                Log.d(TAG, "onNewIntent() version:" + intent.getExtras().getInt("version"));
                notificationVersion(intent.getExtras().getInt("version"));
                // TODO(nic) resendLastDataToPebble();
            }
        }
    }

    private void notificationVersion(int version) {
        if (version < Constants.LAST_VERSION_PEBBLE) {
            Log.d(TAG, "version:" + version + " min:" + Constants.MIN_VERSION_PEBBLE + " last:" + Constants.LAST_VERSION_PEBBLE);
            String message = getString(R.string.message_pebble_new_watchface);

            showToast(message);
            sendMessageToPebble(message);
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private void sendMessageToPebble(String message) {
        _bus.post(new NewMessage(message));
    }

    // TODO: move to pebble service
    private void changeState(int button) {
        Log.d(TAG, "changeState(button:" + button + ")");
        switch (button) {
            case Constants.STOP_PRESS:
                _serviceStarter.stopLocationServices();
                break;
            case Constants.PLAY_PRESS:
                _serviceStarter.startLocationServices();
                break;
            case Constants.REFRESH_PRESS:
                _bus.post(new com.njackson.events.GPSService.ResetGPSState());
                break;
        }
    }
}
