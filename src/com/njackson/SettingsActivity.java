package com.njackson;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

/**
 * Created with IntelliJ IDEA.
 * User: njackson
 * Date: 31/07/2013
 * Time: 22:43
 * To change this template use File | Settings | File Templates.
 */
public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    public static final String KEY_PREF_SYNC_CONN = "pref_syncConnectionType";
    

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    	Log.d("PebbleBike:SettingsActivity", "onSharedPreferenceChanged:" + key);
        if (key.equals(KEY_PREF_SYNC_CONN)) {
            Preference connectionPref = findPreference(key);
            // Set summary to be the user-description for the selected value
            connectionPref.setSummary(sharedPreferences.getString(key, ""));
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        Preference pref = findPreference("pref_install");
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                // TODO Auto-generated method stub
            	Log.d("PebbleBike:SettingsActivity", "onPreferenceClick:" + preference.getKey());
            	Log.d("PebbleBike:SettingsActivity", "onPreferenceClick:" + preference.getTitle());
            	return false;
            }
        });

    }

}
