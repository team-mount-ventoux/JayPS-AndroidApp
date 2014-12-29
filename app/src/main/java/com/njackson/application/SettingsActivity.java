package com.njackson.application;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
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

    private static final String TAG = "PB-SettingsActivity";

    @Inject IInstallWatchFace _installWatchFace;
    @Inject SharedPreferences _sharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //PreferenceManager prefMgr = getPreferenceManager();
        //prefMgr.setSharedPreferencesName("default");
        //prefMgr.setSharedPreferencesMode(MODE_PRIVATE);

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
    private void _setUnitsSummary() {
        String units = _sharedPreferences.getString("UNITS_OF_MEASURE", "0");
        Preference unitsPref = findPreference("UNITS_OF_MEASURE");
        unitsPref.setSummary(units.equals("0") ? getString(R.string.PREF_UNITS_UNIT_IMPERIAL) : getString(R.string.PREF_UNITS_UNIT_METRIC));
    }
    private void _setRefreshSummary() {
        try {
            int refresh_interval = Integer.valueOf(_sharedPreferences.getString("REFRESH_INTERVAL", "1000"));
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
    private void _setLoginJaypsSummary() {
        String login = _sharedPreferences.getString("LIVE_TRACKING_LOGIN", "");
        Preference loginPref = findPreference("LIVE_TRACKING_LOGIN");
        loginPref.setSummary(login);
    }
    private void _setLoginMmtSummary() {
        String login = _sharedPreferences.getString("LIVE_TRACKING_MMT_LOGIN", "");
        Preference loginPref = findPreference("LIVE_TRACKING_MMT_LOGIN");
        loginPref.setSummary(login);
    }
    private void _setOruxMapsSummary() {
        ListPreference oruxPref = (ListPreference) findPreference("ORUXMAPS_AUTO");
        CharSequence listDesc = oruxPref.getEntry();
        oruxPref.setSummary(listDesc);
    }
    private void _setCanvasSummary() {
        ListPreference canvasPref = (ListPreference) findPreference("CANVAS_MODE");
        CharSequence listDesc = canvasPref.getEntry();
        canvasPref.setSummary(listDesc);
    }
    private void _setHrmSummary() {
        String summary = "";
        // TODO(jay) String summary = MainActivity.hrm_name;
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

        _setUnitsSummary();
        _setRefreshSummary();
        _setLoginJaypsSummary();
        _setLoginMmtSummary();
        _setOruxMapsSummary();
        _setCanvasSummary();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        Log.i(TAG, "onSharedPreferenceChanged");
        if (s.equals("UNITS_OF_MEASURE")) {
            _setUnitsSummary();
        }
        if (s.equals("REFRESH_INTERVAL")) {
            _setRefreshSummary();
        }
        if (s.equals("LIVE_TRACKING_LOGIN")) {
            _setLoginJaypsSummary();
        }
        if (s.equals("LIVE_TRACKING_MMT_LOGIN")) {
            _setLoginMmtSummary();
        }
        if (s.equals("ORUXMAPS_AUTO")) {
            _setOruxMapsSummary();
        }
        if (s.equals("CANVAS_MODE")) {
            _setCanvasSummary();
        }
    }
}
