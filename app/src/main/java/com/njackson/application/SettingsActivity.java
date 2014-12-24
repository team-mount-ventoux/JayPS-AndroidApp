package com.njackson.application;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import com.njackson.R;
import com.njackson.application.modules.PebbleBikeApplication;
import com.njackson.utils.IInstallWatchFace;
import com.njackson.utils.ToastMessageMaker;
import com.njackson.utils.pebble.InstallWatchFace;

import javax.inject.Inject;

/**
 * Created by server on 28/06/2014.
 */
public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject IInstallWatchFace _installWatchFace;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((PebbleBikeApplication)getApplication()).inject(this);

        addPreferencesFromResource(R.xml.preferences);

        Preference installPreference = findPreference("pref_install");
        installPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                _installWatchFace.execute(getApplicationContext(), new ToastMessageMaker());
                return true;
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

    }
}
