package com.njackson.activities;

import android.content.Intent;
import android.content.SharedPreferences;
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
import com.njackson.events.ActivityRecognitionService.CurrentState;
import com.njackson.events.UI.StartButtonTouchedEvent;
import com.njackson.events.UI.StopButtonTouchedEvent;
import com.njackson.utils.services.IServiceStarter;
import com.njackson.virtualpebble.IMessageManager;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

public class MainActivity extends FragmentActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "PB-MainActivity";
    @Inject Bus _bus;
    @Inject IAnalytics _analytics;
    @Inject SharedPreferences _sharedPreferences;
    @Inject IServiceStarter _serviceStarter;
    @Inject IMessageManager _messageManager;

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
            Log.d(TAG, "STARTED");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((PebbleBikeApplication) getApplication()).inject(this);

        setContentView(R.layout.activity_main);

        _analytics.trackAppOpened(getIntent());
        _serviceStarter.startPebbleService();

        if (getIntent().getExtras() != null) {
            if (getIntent().getExtras().containsKey("button")) {
                Log.d(TAG, "onCreate() button:" + getIntent().getExtras().getInt("button"));
                changeState(getIntent().getExtras().getInt("button"));
            }
            if (getIntent().getExtras().containsKey("version")) {
                Log.d(TAG, "onCreate() version:" + getIntent().getExtras().getInt("version"));
                notificationVersion(getIntent().getExtras().getInt("version"));
                // TODO(nic) resendLastDataToPebble();
            }
        }
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
        _serviceStarter.stopPebbleService();
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

    // This is called for activities that set launchMode to "singleTop" in their package, or if a client used the FLAG_ACTIVITY_SINGLE_TOP flag when calling startActivity(Intent).
    // In either case, when the activity is re-launched while at the top of the activity stack instead of a new instance of the activity being started, onNewIntent() will be called
    // on the existing instance with the Intent that was used to re-launch it.
    // An activity will always be paused before receiving a new intent, so you can count on onResume() being called after this method.
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
            String msg = "A new watchface is available. Please install it from the Pebble Bike android application settings";
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            if (version < Constants.MIN_VERSION_PEBBLE) {
                _messageManager.showSimpleNotificationOnWatch("Pebble Bike", msg);
            }
        }
    }
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
                // TODO(nic): works only if GPS is running
                _bus.post(new com.njackson.events.GPSService.ResetGPSState());
                break;
        }
    }
}
