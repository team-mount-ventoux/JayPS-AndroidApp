package com.njackson.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;

import com.njackson.Constants;
import com.njackson.R;
import com.njackson.activities.HRMScanActivity;
import com.njackson.application.PebbleBikeApplication;
import com.njackson.utils.watchface.IInstallWatchFace;
import com.njackson.utils.messages.ToastMessageMaker;

import javax.inject.Inject;

import de.cketti.library.changelog.ChangeLog;

/**
 * Created by server on 28/06/2014.
 */
public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "PB-SettingsActivity";

    @Inject IInstallWatchFace _installWatchFace;
    @Inject SharedPreferences _sharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((PebbleBikeApplication)getApplication()).inject(this);

        addPreferencesFromResource(R.xml.preferences);

        Preference installPreference = findPreference("INSTALL_WATCHFACE");
        installPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                _installWatchFace.execute(getApplicationContext(), new ToastMessageMaker());
                return true;
            }
        });

        final ChangeLog cl = new ChangeLog(this);
        Preference changelog = findPreference("CHANGE_LOG");
        changelog.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                cl.getFullLogDialog().show();
                return true;
            }
        });

        Preference pref = findPreference("PREF_PRESSURE_INFO");
        SensorManager mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null){
            pref.setSummary("Pressure sensor available");
        } else {
            pref.setSummary("No pressure sensor");
        }

        pref = findPreference("PREF_GEOID_HEIGHT_INFO");
        if (_sharedPreferences.getFloat("GEOID_HEIGHT", 0) != 0) {
            pref.setSummary("Correction: " + _sharedPreferences.getFloat("GEOID_HEIGHT", 0) + "m");
        } else {
            pref.setSummary("No correction");
        }

        setHrmSummary();

        // check to determine whether BLE is supported on the device.
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Preference pref_hrm = findPreference("PREF_HRM");
            pref_hrm.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (preference.getKey().equals("PREF_HRM")) {
                        final Intent intent = new Intent(getApplicationContext(), HRMScanActivity.class);
                        startActivityForResult(intent, 1);
                    }
                    return false;
                }
            });
        }
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            String hrm_name = "";
            String hrm_address = "";
            if(resultCode == RESULT_OK) {
                hrm_name = data.getStringExtra("hrm_name");
                hrm_address = data.getStringExtra("hrm_address");
            }

            SharedPreferences.Editor editor = _sharedPreferences.edit();
            editor.putString("hrm_name", hrm_name);
            editor.putString("hrm_address", hrm_address);
            editor.commit();

            setHrmSummary();

            if (!hrm_address.equals("")) {
                //todo(jay) if (MainActivity.getInstance().checkServiceRunning()) {
                    Toast.makeText(getApplicationContext(), "Please restart GPS to display heart rate", Toast.LENGTH_LONG).show();
                //}
            }
        }
    }

	@Override
    protected void onResume() {
        super.onResume();

        _sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        setUnitsSummary();
        setRefreshSummary();
        setLoginJaypsSummary();
        setLoginMmtSummary();
        setOruxMapsSummary();
        setCanvasSummary();
    }

    @Override
    protected void onPause() {
        _sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);

        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        Log.i(TAG, "onSharedPreferenceChanged");
        if (s.equals("UNITS_OF_MEASURE")) {
            setUnitsSummary();
        }
        if (s.equals("REFRESH_INTERVAL")) {
            setRefreshSummary();
        }
        if (s.equals("LIVE_TRACKING_LOGIN")) {
            setLoginJaypsSummary();
        }
        if (s.equals("LIVE_TRACKING_MMT_LOGIN")) {
            setLoginMmtSummary();
        }
        if (s.equals("ORUXMAPS_AUTO")) {
            setOruxMapsSummary();
        }
        if (s.equals("CANVAS_MODE")) {
            setCanvasSummary();
        }
    }

    private void setUnitsSummary() {
        String units = _sharedPreferences.getString("UNITS_OF_MEASURE", "0");
        Preference unitsPref = findPreference("UNITS_OF_MEASURE");

        if(units.equals("0")) {
            unitsPref.setSummary(getString(R.string.PREF_UNITS_UNIT_IMPERIAL));
        } else {
            unitsPref.setSummary(getString(R.string.PREF_UNITS_UNIT_METRIC));
        }
    }

    private void setRefreshSummary() {
        int refresh_interval = 0;

        try {
            refresh_interval = Integer.valueOf(_sharedPreferences.getString("REFRESH_INTERVAL", "500"));
        }catch (NumberFormatException nfe) {
            refresh_interval = Constants.REFRESH_INTERVAL_DEFAULT;
        }

        Preference refreshPref = findPreference("REFRESH_INTERVAL");

        if (refresh_interval < 1000) {
            refreshPref.setSummary(refresh_interval + " ms");
        } else {
            refreshPref.setSummary(refresh_interval/1000 + " s");
        }
    }

    private void setLoginJaypsSummary() {
        String login = _sharedPreferences.getString("LIVE_TRACKING_LOGIN", "");
        Preference loginPref = findPreference("LIVE_TRACKING_LOGIN");
        loginPref.setSummary(login);
    }

    private void setLoginMmtSummary() {
        String login = _sharedPreferences.getString("LIVE_TRACKING_MMT_LOGIN", "");
        Preference loginPref = findPreference("LIVE_TRACKING_MMT_LOGIN");
        loginPref.setSummary(login);
    }

    private void setOruxMapsSummary() {
        ListPreference oruxPref = (ListPreference) findPreference("ORUXMAPS_AUTO");
        CharSequence listDesc = oruxPref.getEntry();
        oruxPref.setSummary(listDesc);
    }

    private void setCanvasSummary() {
        ListPreference canvasPref = (ListPreference) findPreference("CANVAS_MODE");
        CharSequence listDesc = canvasPref.getEntry();
        canvasPref.setSummary(listDesc);
    }

    private void setHrmSummary() {
        String summary = _sharedPreferences.getString("hrm_name", "");
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            summary = getResources().getString(R.string.ble_not_supported);
        }
        if (summary.equals("")) {
            summary = "Click to choose a sensor";
        }
        Preference loginPref = findPreference("PREF_HRM");
        loginPref.setSummary(summary);
    }
}
