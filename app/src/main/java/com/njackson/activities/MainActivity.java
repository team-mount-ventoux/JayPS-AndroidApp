package com.njackson.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.njackson.R;
import com.njackson.activityrecognition.ActivityRecognitionService;
import com.njackson.analytics.IAnalytics;
import com.njackson.application.PebbleBikeApplication;
import com.njackson.application.SettingsActivity;
import com.njackson.events.UI.StartButtonTouchedEvent;
import com.njackson.events.UI.StopButtonTouchedEvent;
import com.njackson.gps.GPSService;
import com.njackson.live.LiveService;
import com.njackson.virtualpebble.PebbleService;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import javax.inject.Inject;

public class MainActivity extends FragmentActivity {

    private static final String TAG = "MainActivity";
    @Inject Bus _bus;
    @Inject IAnalytics _analytics;

    @Subscribe
    public void onStartButtonTouched(StartButtonTouchedEvent event) {
        Log.d("MAINTEST", "Button Clicked");
        startGPSService();
        startPebbleService();
        startLiveService();
    }

    @Subscribe
    public void onStopButtonTouched(StopButtonTouchedEvent event) {
        stopGPSService();
        stopPebbleService();
        stopLiveService();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((PebbleBikeApplication) getApplication()).inject(this);
        _bus.register(this);

        setContentView(R.layout.activity_main);

        _analytics.trackAppOpened(getIntent());

        startActivityRecognitionService();
    }

    @Override
    protected void onDestroy() {
        Log.d("MAINTEST", "Bus un-registered");
        _bus.unregister(this);
        super.onDestroy();
    }

    protected void startGPSService() {
        startService(new Intent(this,GPSService.class));
    }

    private void stopGPSService() {
        stopService(new Intent(this,GPSService.class));
    }

    private void startPebbleService() { startService(new Intent(this, PebbleService.class)); }

    private void stopPebbleService() {
        stopService(new Intent(this,PebbleService.class));
    }

    private void startLiveService() { startService(new Intent(this, LiveService.class)); }

    private void stopLiveService() {
        stopService(new Intent(this,LiveService.class));
    }

    private void startActivityRecognitionService() {
        startService(new Intent(this, ActivityRecognitionService.class));
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

}
