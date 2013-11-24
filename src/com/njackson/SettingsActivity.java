package com.njackson;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
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
                if (preference.getKey().equals("pref_install")) {
                    install_watchface(1);
                }
                if (preference.getKey().equals("pref_install_sdk2")) {
                    install_watchface(2);
                }
                return false;
            }
        };
        
        Preference pref = findPreference("pref_install");
        pref.setOnPreferenceClickListener(pref_install_click_listener);
        if (MainActivity.pebbleFirmwareVersion == 1) {
            pref.setTitle(pref.getTitle() + " [your version]");
            pref.setSummary(pref.getSummary() + " This is the version compatible with your current Pebble firmware.");
        }

        Preference pref2 = findPreference("pref_install_sdk2");
        pref2.setOnPreferenceClickListener(pref_install_click_listener);
        if (MainActivity.pebbleFirmwareVersion == 2) {
            pref2.setTitle(pref2.getTitle() + " [your version]");
            pref2.setSummary(pref2.getSummary() + " This is the version compatible with your current Pebble firmware.");
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
            String uriString = "http://labs.jayps.fr/pebblebike/pebblebike-1.3.0";
            if (sdkVersion == 2) {
                uriString += "-sdk2";
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
    private void _setLoginSummary(SharedPreferences prefs) {
        String login = prefs.getString("LIVE_TRACKING_LOGIN", "");
        Preference loginPref = findPreference("LIVE_TRACKING_LOGIN");
        loginPref.setSummary(login);
    }
	@Override
    protected void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        
        _setUnitsSummary(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        _setRefreshSummary(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        _setLoginSummary(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
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
            _setLoginSummary(sharedPreferences);
        }
        
        MainActivity activity = MainActivity.getInstance();
        if(activity != null)
            activity.loadPreferences(sharedPreferences);
    }

}
