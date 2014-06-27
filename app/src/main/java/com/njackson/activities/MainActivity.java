package com.njackson.activities;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.njackson.R;
import com.njackson.application.PebbleBikeApplication;
import com.njackson.gps.GPSService;
import com.squareup.otto.Bus;

import javax.inject.Inject;

public class MainActivity extends FragmentActivity {

    @Inject Bus _bus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((PebbleBikeApplication) getApplication()).inject(this);

        setContentView(R.layout.activity_main);

        startGPSService();
    }

    private void startGPSService() {
        startService(new Intent(this,GPSService.class));
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
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
