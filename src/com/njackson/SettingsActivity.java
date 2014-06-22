package com.njackson;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Created with IntelliJ IDEA.
 * User: njackson
 * Date: 31/07/2013
 * Time: 22:43
 * To change this template use File | Settings | File Templates.
 */
public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	
	private static final String TAG = "PB-SettingsActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        OnPreferenceClickListener pref_install_click_listener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // TODO Auto-generated method stub
                Log.d(TAG, "onPreferenceClick:" + preference.getKey());
                if (preference.getKey().equals("pref_install_sdk2")) {
                    install_watchface(2);
                }
                if (preference.getKey().equals("pref_reset_data")) {
                    MainActivity activity = MainActivity.getInstance();
                    if (activity != null) {
                        activity.ResetSavedGPSStats();
                    }
                }
                if (preference.getKey().equals("PREF_HRM")) {
                    final Intent intent = new Intent(getApplicationContext(), HRMScanActivity.class);
                    startActivity(intent);                    
                }
                return false;
            }
        };
        
        Preference pref;
        Preference pref2 = findPreference("pref_install_sdk2");
        pref2.setOnPreferenceClickListener(pref_install_click_listener);
        Preference pref3 = findPreference("pref_reset_data");
        pref3.setOnPreferenceClickListener(pref_install_click_listener);


        pref = findPreference("PREF_PRESSURE_INFO");
        SensorManager mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null){
            pref.setSummary("Pressure sensor available");
        } else {
            pref.setSummary("No pressure sensor");
        }

        pref = findPreference("PREF_GEOID_HEIGHT_INFO");
        if (MainActivity.geoidHeight != 0) {
            pref.setSummary("Correction: " + MainActivity.geoidHeight + "m");
        } else {
            pref.setSummary("No correction");
        }

        _setHrmSummary();

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

           SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME,0);
           SharedPreferences.Editor editor = settings.edit();
           editor.putString("hrm_name", hrm_name);
           editor.putString("hrm_address", hrm_address);
           editor.commit();

           // reload prefs
           MainActivity.getInstance().loadPreferences();

           _setHrmSummary();

           if (!hrm_address.equals("")) {
               if (MainActivity.getInstance().checkServiceRunning()) {
                   Toast.makeText(getApplicationContext(), "Please restart GPS to display heart rate", Toast.LENGTH_LONG).show();
               }
           }
        }
    }
    private boolean install_watchface(int sdkVersion) {
        int versionCode;
    
        // Get current version code and version name
        try {
            PackageInfo packageInfo = getApplicationContext().getPackageManager().getPackageInfo(
                    getApplicationContext().getPackageName(), 0);
    
            versionCode = packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            versionCode = 0;
        }
        Log.d(TAG, "versionCode:" + versionCode);
        Log.d(TAG, "sdkVersion:" + sdkVersion);
        Log.d(TAG, "peebleFirmwareVersion:" + MainActivity.pebbleFirmwareVersion);
        
        try {
            String uriString;
            if (sdkVersion == 2) {
                uriString = "http://dl.pebblebike.com/p/pebblebike-1.4.0";
            } else {
                uriString = "http://labs.jayps.fr/pebblebike/pebblebike-1.3.0";
            }
            uriString += ".pbw?and&v=" + versionCode;
            Log.d(TAG, "uriString:" + uriString);
            Uri uri = Uri.parse(uriString);
            Intent startupIntent = new Intent();
            startupIntent.setAction(Intent.ACTION_VIEW);
            startupIntent.setType("application/octet-stream");
            startupIntent.setData(uri);
            ComponentName distantActivity = new ComponentName("com.getpebble.android", "com.getpebble.android.ui.UpdateActivity");
            startupIntent.setComponent(distantActivity);
            startActivity(startupIntent);
        } catch (ActivityNotFoundException ae) {
            Toast.makeText(getApplicationContext(),"Unable to install watchface, do you have the latest pebble app installed?",Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }
    
    private void _setUnitsSummary(SharedPreferences prefs) {
        String units = prefs.getString("UNITS_OF_MEASURE", "0");
        Preference unitsPref = findPreference("UNITS_OF_MEASURE");
        unitsPref.setSummary(units.equals("0") ? getString(R.string.PREF_UNITS_UNIT_IMPERIAL) : getString(R.string.PREF_UNITS_UNIT_METRIC));
    }
    private void _setRefreshSummary(SharedPreferences prefs) {
        try {
            int refresh_interval = Integer.valueOf(prefs.getString("REFRESH_INTERVAL", "1000"));
            Preference refreshPref = findPreference("REFRESH_INTERVAL");
            if (refresh_interval < 1000) {
                refreshPref.setSummary(refresh_interval + " ms");
            } else {
                refreshPref.setSummary(refresh_interval/1000 + " s");
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception converting REFRESH_INTERVAL:" + e);
        }
    }
    private void _setLoginJaypsSummary(SharedPreferences prefs) {
        String login = prefs.getString("LIVE_TRACKING_LOGIN", "");
        Preference loginPref = findPreference("LIVE_TRACKING_LOGIN");
        loginPref.setSummary(login);
    }
    private void _setLoginMmtSummary(SharedPreferences prefs) {
        String login = prefs.getString("LIVE_TRACKING_MMT_LOGIN", "");
        Preference loginPref = findPreference("LIVE_TRACKING_MMT_LOGIN");
        loginPref.setSummary(login);
    }    
    private void _setOruxMapsSummary(SharedPreferences prefs) {
        ListPreference oruxPref = (ListPreference) findPreference("ORUXMAPS_AUTO");
        CharSequence listDesc = oruxPref.getEntry();
        oruxPref.setSummary(listDesc);
    }
    private void _setHrmSummary() {
        String summary = MainActivity.hrm_name;
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            summary = getResources().getString(R.string.ble_not_supported);
        }
        if (summary.equals("")) {
            summary = "Click to choose a sensor";
        }
        Preference loginPref = findPreference("PREF_HRM");
        loginPref.setSummary(summary);
    }

	@Override
    protected void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        
        _setUnitsSummary(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        _setRefreshSummary(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        _setLoginJaypsSummary(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        _setLoginMmtSummary(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        _setOruxMapsSummary(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        
        if (key.equals("UNITS_OF_MEASURE")) {
            _setUnitsSummary(sharedPreferences);
        }        
        if (key.equals("REFRESH_INTERVAL")) {
            _setRefreshSummary(sharedPreferences);
        }
        if (key.equals("LIVE_TRACKING_LOGIN")) {
            _setLoginJaypsSummary(sharedPreferences);
        }
        if (key.equals("LIVE_TRACKING_MMT_LOGIN")) {
            _setLoginMmtSummary(sharedPreferences);
        }
        if (key.equals("ORUXMAPS_AUTO")) {
            _setOruxMapsSummary(sharedPreferences);
        }

        MainActivity activity = MainActivity.getInstance();
        if(activity != null)
            activity.loadPreferences(sharedPreferences);
    }

}
