package com.njackson.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;

import com.njackson.Constants;
import com.njackson.R;
import com.njackson.application.PebbleBikeApplication;
import com.njackson.events.BleServiceCommand.BleSensorData;
import com.njackson.events.GPSServiceCommand.ChangeRefreshInterval;
import com.njackson.events.GPSServiceCommand.ResetGPSState;
import com.njackson.gps.Navigator;
import com.njackson.state.IGPSDataStore;
import com.njackson.upload.RunkeeperUpload;
import com.njackson.upload.StravaUpload;
import com.njackson.utils.gpx.GpxExport;
import com.njackson.utils.services.IServiceStarter;
import com.njackson.utils.watchface.IInstallWatchFace;
import com.njackson.utils.messages.ToastMessageMaker;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.inject.Inject;

import de.cketti.library.changelog.ChangeLog;
import fr.jayps.android.AdvancedLocation;

/**
 * Created by server on 28/06/2014.
 */
public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "PB-SettingsActivity";

    @Inject IInstallWatchFace _installWatchFace;
    @Inject SharedPreferences _sharedPreferences;
    @Inject IGPSDataStore _dataStore;
    @Inject IServiceStarter _serviceStarter;
    @Inject Bus _bus;
    @Inject Navigator _navigator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((PebbleBikeApplication)getApplication()).inject(this);

        addPreferencesFromResource(R.xml.preferences);

        Preference installPreference = findPreference("INSTALL_WATCHFACE");
        installPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                _installWatchFace.execute(getApplicationContext(), new ToastMessageMaker(), getApplicationContext().getString(R.string.PREF_INSTALL_WATCHFACE_URL));
                return true;
            }
        });
        Preference installPreferenceNl = findPreference("INSTALL_WATCHFACE_NL");
        installPreferenceNl.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                _installWatchFace.execute(getApplicationContext(), new ToastMessageMaker(), getApplicationContext().getString(R.string.PREF_INSTALL_WATCHFACE_NL_URL));
                return true;
            }
        });

        Preference resetPreference = findPreference("RESET_DATA");
        resetPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(preference.getContext())
                        .setTitle(R.string.ALERT_RESET_DATA_TITLE)
                        .setMessage(R.string.ALERT_RESET_DATA_MESSAGE)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                _dataStore.resetAllValues();
                                _dataStore.commit();
                                _bus.post(new ResetGPSState());
                                AdvancedLocation advancedLocation = new AdvancedLocation(getApplicationContext());
                                advancedLocation.resetGPX();
                                Toast.makeText(getApplicationContext(), "Done", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            }
        });
        Preference exportGPXPreference = findPreference("EXPORT_GPX");
        exportGPXPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                GpxExport.export(getApplicationContext(), _sharedPreferences.getBoolean("ADVANCED_GPX", false));
                return true;
            }
        });
        Preference resetGPXPreference = findPreference("RESET_TRACKS");
        resetGPXPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(preference.getContext())
                        .setTitle(R.string.ALERT_RESET_TRACKS_TITLE)
                        .setMessage(R.string.ALERT_RESET_TRACKS_MESSAGE)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                AdvancedLocation advancedLocation = new AdvancedLocation(getApplicationContext());
                                advancedLocation.resetGPX();
                                Toast.makeText(getApplicationContext(), "Done", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null).show();
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
        // note: double check FEATURE_BLUETOOTH_LE + android version because the 1st test (hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) seems to return true on some 4.1 & 4.2
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
            && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2 // BLE requires 4.3 (Api level 18)
        ) {

            Preference pref_ble1 = findPreference("PREF_BLE1");
            pref_ble1.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (preference.getKey().equals("PREF_BLE1")) {
                        final Intent intent = new Intent(getApplicationContext(), HRMScanActivity.class);
                        startActivityForResult(intent, 1);
                    }
                    return false;
                }
            });

            Preference pref_ble2 = findPreference("PREF_BLE2");
            pref_ble2.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (preference.getKey().equals("PREF_BLE2")) {
                        final Intent intent = new Intent(getApplicationContext(), HRMScanActivity.class);
                        startActivityForResult(intent, 2);
                    }
                    return false;
                }
            });

            Preference pref_ble3 = findPreference("PREF_BLE3");
            pref_ble3.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (preference.getKey().equals("PREF_BLE3")) {
                        final Intent intent = new Intent(getApplicationContext(), HRMScanActivity.class);
                        startActivityForResult(intent, 3);
                    }
                    return false;
                }
            });
        }
        final Activity _activity = this;
        Preference pref_nav_load_route = findPreference("PREF_LOAD_ROUTE");
        pref_nav_load_route.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (preference.getKey().equals("PREF_LOAD_ROUTE")) {
                    _navigator.debugLevel = _sharedPreferences.getBoolean("PREF_DEBUG", false) ? 1 : 0;
                    Toast.makeText(getApplicationContext(), "Open a GPX file", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    try {
                        startActivityForResult(Intent.createChooser(intent, "Select txt file"), Constants.CODE_LOAD_GPX);
                    } catch (android.content.ActivityNotFoundException ex) {
                        // Potentially direct the user to the Market with a Dialog
                        Toast.makeText(getApplicationContext(), "Impossible to open file", Toast.LENGTH_SHORT).show();
                    }
                }
                return false;
            }
        });
        Preference pref_nav_stop = findPreference("PREF_NAV_STOP");
        pref_nav_stop.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (preference.getKey().equals("PREF_NAV_STOP")) {
                    if (_navigator.getNbPoints() > 0) {
                        _navigator.clearRoute();
                        Toast.makeText(getApplicationContext(), "Navigation was stopped", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "The navigation was not started", Toast.LENGTH_SHORT).show();
                    }
                }
                return false;
            }
        });



        Preference pref_nav_export_route_orux = findPreference("PREF_EXPORT_ROUTE_ORUX");
        pref_nav_export_route_orux.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (preference.getKey().equals("PREF_EXPORT_ROUTE_ORUX")) {
                    _navigator.debugLevel = _sharedPreferences.getBoolean("PREF_DEBUG", false) ? 1 : 0;
                    _navigator.loadRouteToOrux(_activity);
                }
                return false;
            }
        });
        Preference pref_nav_open_planner = findPreference("PREF_NAV_PLANNER");
        pref_nav_open_planner.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String uriString = getApplicationContext().getString(R.string.PREF_NAV_PLANNER_URL);
                String position = "";
                if (_dataStore.getLastLocationLatitude() != 0.0f && _dataStore.getLastLocationLongitude() != 0.0f) {
                    position = _dataStore.getLastLocationLatitude() + "," + _dataStore.getLastLocationLongitude();
                }
                //Log.d(TAG, "position:" + position);
                if (!_sharedPreferences.getString("PREF_NAV_PLANNER_START", "").isEmpty()) {
                    if (_sharedPreferences.getString("PREF_NAV_PLANNER_START", "").equals("POSITION")) {
                        uriString += "&start=" + position;
                    } else {
                        uriString += "&start=" + _sharedPreferences.getString("PREF_NAV_PLANNER_START", "");
                    }
                }
                if (!_sharedPreferences.getString("PREF_NAV_PLANNER_END", "").isEmpty()) {
                    if (_sharedPreferences.getString("PREF_NAV_PLANNER_END", "").equals("POSITION")) {
                        uriString += "&end=" + position;
                    } else {
                        uriString += "&end=" + _sharedPreferences.getString("PREF_NAV_PLANNER_END", "");
                    }
                }
                Log.d(TAG, "uriString:" + uriString);

                Uri uri = Uri.parse(uriString);

                Intent startupIntent = new Intent();
                startupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startupIntent.setAction(Intent.ACTION_VIEW);
                startupIntent.setData(uri);

                startActivity(startupIntent);

                return true;
            }
        });

        Preference pref_strava = findPreference("PREF_STRAVA");
        pref_strava.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (preference.getKey().equals("PREF_STRAVA")) {
                    Intent mIntent = new Intent(getApplicationContext(), UploadActivity.class);
                    mIntent.putExtra("type", "strava");
                    startActivity(mIntent);
                }
                return false;
            }
        });
        Preference pref_runkeeper = findPreference("PREF_RUNKEEPER");
        pref_runkeeper.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (preference.getKey().equals("PREF_RUNKEEPER")) {
                    Intent mIntent = new Intent(getApplicationContext(), UploadActivity.class);
                    mIntent.putExtra("type", "runkeeper");
                    startActivity(mIntent);
                }
                return false;
            }
        });
        Preference pref_upload_strava = findPreference("PREF_UPLOAD_STRAVA");
        pref_upload_strava.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (_sharedPreferences.getBoolean("ENABLE_TRACKS", false)) {
                    if (!_sharedPreferences.getString("strava_token", "").isEmpty()) {
                        StravaUpload strava_upload = new StravaUpload(_activity);
                        strava_upload.upload(_sharedPreferences.getString("strava_token", ""));
                    } else {
                        Toast.makeText(getApplicationContext(), "Please configure Strava in the settings before using the upload", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please enable tracks in the settings to save GPX before using the upload to Strava", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
        Preference pref_upload_runkeeper = findPreference("PREF_UPLOAD_RUNKEEPER");
        pref_upload_runkeeper.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (_sharedPreferences.getBoolean("ENABLE_TRACKS", false)) {
                    if (!_sharedPreferences.getString("runkeeper_token", "").isEmpty()) {
                        RunkeeperUpload runkeeper_upload = new RunkeeperUpload(_activity);
                        runkeeper_upload.upload(_sharedPreferences.getString("runkeeper_token", ""));
                    } else {
                        Toast.makeText(getApplicationContext(), "Please configure Runkeeper in the settings before using the upload", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please enable tracks in the settings to save GPX before using the upload to Runkeeper", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.CODE_LOAD_GPX) {
            if (data != null) {
                try {
                    Uri uri = data.getData();
                    BufferedReader br = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
                    StringBuilder gpx = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        gpx.append(line).append('\n');
                    }
                    _navigator.loadGpx(gpx.toString());
                    Toast.makeText(getApplicationContext(), "Route loaded - " + _navigator.getNbPoints() + " points", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Exception:" + e);
                }
            }
        } else if (requestCode > 0) {
            int sensorNumber = requestCode;
            Log.d(TAG, "onActivityResult sensorNumber="+sensorNumber);

            String hrm_name = "";
            String hrm_address = "";
            if(resultCode == RESULT_OK) {
                hrm_name = data.getStringExtra("hrm_name");
                hrm_address = data.getStringExtra("hrm_address");
            }

            SharedPreferences.Editor editor = _sharedPreferences.edit();
            if (sensorNumber == 2) {
                editor.putString("hrm_name2", hrm_name);
                editor.putString("hrm_address2", hrm_address);
            } else if (sensorNumber == 3) {
                editor.putString("hrm_name3", hrm_name);
                editor.putString("hrm_address3", hrm_address);
            } else {
                editor.putString("hrm_name", hrm_name);
                editor.putString("hrm_address", hrm_address);
            }
            editor.commit();

            setHrmSummary();

            if (!hrm_address.equals("")) {
                if (_serviceStarter.isLocationServicesRunning()) {
                    Toast.makeText(getApplicationContext(), "Please restart GPS to display BLE sensor data", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    @Subscribe
    public void onNewBleSensorData(BleSensorData event) {
        String key = "PREF_BLE1";
        int bleNumber = 1;
        if (event.getBleAddress().equals(_sharedPreferences.getString("hrm_address2", ""))) {
            key = "PREF_BLE2";
            bleNumber = 2;
        } else if (event.getBleAddress().equals(_sharedPreferences.getString("hrm_address3", ""))) {
            key = "PREF_BLE3";
            bleNumber = 3;
        }
        switch (event.getType()) {
            case BleSensorData.SENSOR_HRM:
                setBleTitle(getApplicationContext().getString(R.string.PREF_BLE_TITLE) + " " + bleNumber + " - Heart rate: " + event.getHeartRate(), key);
                break;
            case BleSensorData.SENSOR_CSC_CADENCE:
                setBleTitle(getApplicationContext().getString(R.string.PREF_BLE_TITLE) + " " + bleNumber +  " - Cadence: " + event.getCyclingCadence(), key);
                break;
            case BleSensorData.SENSOR_RSC:
                setBleTitle(getApplicationContext().getString(R.string.PREF_BLE_TITLE) + " " + bleNumber + " - Cadence: " + event.getRunningCadence(), key);
                break;
        }
    }

	@Override
    protected void onResume() {
        super.onResume();
        _bus.register(this);

        _sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        setUnitsSummary();
        setRefreshSummary(_sharedPreferences.getString("REFRESH_INTERVAL", String.valueOf(Constants.REFRESH_INTERVAL_DEFAULT)));
        setLoginJaypsSummary();
        setLoginMmtSummary();
        setLiveSummary();
        setStravaSummary();
        setRunkeeperSummary();
        setOruxMapsSummary();
        setCanvasSummary();
        setHrmSummary();
    }

    @Override
    protected void onPause() {
        _sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);

        super.onPause();
        _bus.unregister(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        //Log.i(TAG, "onSharedPreferenceChanged" + s);
        _dataStore.reloadPreferencesFromSettings();
        if (s.equals("UNITS_OF_MEASURE")) {
            setUnitsSummary();
        }
        if (s.equals("REFRESH_INTERVAL")) {
            int refresh_interval = 0;
            try {
                refresh_interval = Integer.valueOf(_sharedPreferences.getString("REFRESH_INTERVAL", String.valueOf(Constants.REFRESH_INTERVAL_DEFAULT)));
            }catch (NumberFormatException nfe) {
                refresh_interval = Constants.REFRESH_INTERVAL_DEFAULT;
            }
            _bus.post(new ChangeRefreshInterval(refresh_interval));
            setRefreshSummary(_sharedPreferences.getString("REFRESH_INTERVAL", String.valueOf(Constants.REFRESH_INTERVAL_DEFAULT)));
        }
        if (s.equals("LIVE_TRACKING") || s.equals("LIVE_TRACKING_MMT")) {
            setLiveSummary();
        }
        if (s.equals("LIVE_TRACKING_LOGIN")) {
            setLiveSummary();
            setLoginJaypsSummary();
        }
        if (s.equals("LIVE_TRACKING_MMT_LOGIN")) {
            setLiveSummary();
            setLoginMmtSummary();
        }
        if (s.equals("LIVE_TRACKING_PASSWORD") || s.equals("LIVE_TRACKING_MMT_PASSWORD")) {
            setLiveSummary();
        }
        if (s.equals("STRAVA_AUTO")) {
            setStravaSummary();
        }
        if (s.equals("RUNKEEPER_AUTO") || s.equals("RUNKEEPER_ACTIVITY_TYPE")) {
            setRunkeeperSummary();
        }
        if (s.equals("ORUXMAPS_AUTO")) {
            setOruxMapsSummary();
        }
        if (s.equals("CANVAS_MODE")) {
            setCanvasSummary();
        }
        if (s.equals("PREF_BLE_HRM_HRMAX")) {
            setHrmSummary();
        }
    }

    private void setUnitsSummary() {
        String units = _sharedPreferences.getString("UNITS_OF_MEASURE", "0");
        Preference unitsPref = findPreference("UNITS_OF_MEASURE");

        if (units.equals("" + Constants.IMPERIAL)) {
            unitsPref.setSummary(getString(R.string.PREF_UNITS_UNIT_IMPERIAL));
        } else if (units.equals(""+Constants.METRIC)) {
            unitsPref.setSummary(getString(R.string.PREF_UNITS_UNIT_METRIC));
        } else if (units.equals(""+Constants.NAUTICAL_IMPERIAL)) {
            unitsPref.setSummary(getString(R.string.PREF_UNITS_UNIT_NAUTICAL_IMPERIAL));
        } else if (units.equals(""+Constants.NAUTICAL_METRIC)) {
            unitsPref.setSummary(getString(R.string.PREF_UNITS_UNIT_NAUTICAL_METRIC));
        } else if (units.equals(""+Constants.RUNNING_IMPERIAL)) {
            unitsPref.setSummary(getString(R.string.PREF_UNITS_UNIT_RUNNING_IMPERIAL));
        } else if (units.equals(""+Constants.RUNNING_METRIC)) {
            unitsPref.setSummary(getString(R.string.PREF_UNITS_UNIT_RUNNING_METRIC));
        }
    }

    private void setRefreshSummary(String p_refreshInterval) {
        ListPreference refreshPref = (ListPreference) findPreference("REFRESH_INTERVAL");
        if (refreshPref.findIndexOfValue(p_refreshInterval) >= 0) {
            CharSequence listDesc = refreshPref.getEntries()[refreshPref.findIndexOfValue(p_refreshInterval)];
            refreshPref.setSummary(listDesc);
        } else {
            // not in the list (old value?)
            int refresh_interval = 0;
            try {
                refresh_interval = Integer.valueOf(_sharedPreferences.getString("REFRESH_INTERVAL", "500"));
            } catch (NumberFormatException nfe) {
                refresh_interval = Constants.REFRESH_INTERVAL_DEFAULT;
            }
            refresh_interval = refresh_interval % 100000;
            if (refresh_interval < 1000) {
                refreshPref.setSummary(refresh_interval + " ms");
            } else {
                refreshPref.setSummary(refresh_interval/1000 + " s");
            }
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

    // TODO(jay) : call me when PreferenceScreen "live_screen" is closed
    private void setLiveSummary() {
        Boolean live_jayps = _sharedPreferences.getBoolean("LIVE_TRACKING", false) && !_sharedPreferences.getString("LIVE_TRACKING_LOGIN", "").equals("") && !_sharedPreferences.getString("LIVE_TRACKING_PASSWORD", "").equals("");
        Boolean live_mmt = _sharedPreferences.getBoolean("LIVE_TRACKING_MMT", false) && !_sharedPreferences.getString("LIVE_TRACKING_MMT_LOGIN", "").equals("") && !_sharedPreferences.getString("LIVE_TRACKING_MMT_PASSWORD", "").equals("");
        Preference live_jayps_screen = findPreference("live_jayps_screen");
        String live = "Disable";
        if (live_jayps) {
            live = "Enable";
        }
        live_jayps_screen.setSummary(live);
        Preference live_mmt_screen = findPreference("live_mmt_screen");
        live = "Disable";
        if (live_mmt) {
            live = "Enable";
        }
        live_mmt_screen.setSummary(live);
    }

    private void setStravaSummary() {
        ListPreference strava_auto = (ListPreference) findPreference("STRAVA_AUTO");
        CharSequence listDesc = strava_auto.getEntry();
        strava_auto.setSummary(listDesc);

        Preference strava_screen = findPreference("strava_screen");
        String strava = "Disable";
        if (!_sharedPreferences.getString("strava_token", "").isEmpty()) {
            if (_sharedPreferences.getString("STRAVA_AUTO", "disable").equals("disable")) {
                strava = "Manual upload";
            } else {
                strava = "Automatic upload";
            }
        }
        strava_screen.setSummary(strava);
    }

    private void setRunkeeperSummary() {
        ListPreference runkeeper_auto = (ListPreference) findPreference("RUNKEEPER_AUTO");
        CharSequence listDesc = runkeeper_auto.getEntry();
        runkeeper_auto.setSummary(listDesc);

        ListPreference runkeeper_activity_type = (ListPreference) findPreference("RUNKEEPER_ACTIVITY_TYPE");
        listDesc = runkeeper_activity_type.getEntry();
        runkeeper_activity_type.setSummary(listDesc);

        Preference runkeeper_screen = findPreference("runkeeper_screen");
        String runkeeper = "Disable";
        if (!_sharedPreferences.getString("runkeeper_token", "").isEmpty()) {
            if (_sharedPreferences.getString("RUNKEEPER_AUTO", "disable").equals("disable")) {
                runkeeper = "Manual upload";
            } else {
                runkeeper = "Automatic upload";
            }
        }
        runkeeper_screen.setSummary(runkeeper);
    }

    private void setOruxMapsSummary() {
        ListPreference oruxPref = (ListPreference) findPreference("ORUXMAPS_AUTO");
        CharSequence listDesc = oruxPref.getEntry();
        oruxPref.setSummary(listDesc);
        Preference orux_screen = findPreference("orux_screen");
        orux_screen.setSummary(listDesc);
    }

    private void setCanvasSummary() {
        ListPreference canvasPref = (ListPreference) findPreference("CANVAS_MODE");
        CharSequence listDesc = canvasPref.getEntry();
        canvasPref.setSummary(listDesc);
        Preference canvas_screen = findPreference("canvas_screen");
        canvas_screen.setSummary(listDesc);
    }
    private void setBleTitle(String title, String key) {
        Preference blePref = findPreference(key);
        blePref.setTitle(title);
    }
    private void setHrmSummary() {
        String summary = _sharedPreferences.getString("hrm_name", "");
        String summary2 = _sharedPreferences.getString("hrm_name2", "");
        String summary3 = _sharedPreferences.getString("hrm_name3", "");
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            summary = getResources().getString(R.string.ble_not_supported);
            summary2 = getResources().getString(R.string.ble_not_supported);
            summary3 = getResources().getString(R.string.ble_not_supported);
        }
        if (summary.equals("")) {
            summary = "Click to choose a sensor";
        }
        Preference hrmPref = findPreference("PREF_BLE1");
        hrmPref.setSummary(summary);

        if (summary2.equals("")) {
            summary2 = "Click to choose a sensor";
        }
        Preference hrmPref2 = findPreference("PREF_BLE2");
        hrmPref2.setSummary(summary2);

        if (summary3.equals("")) {
            summary3 = "Click to choose a sensor";
        }
        Preference hrmPref3 = findPreference("PREF_BLE3");
        hrmPref3.setSummary(summary3);

        Preference hrmMaxPref = findPreference("PREF_BLE_HRM_HRMAX");
        hrmMaxPref.setSummary(_sharedPreferences.getString("PREF_BLE_HRM_HRMAX", getString(R.string.PREF_BLE_HRM_HRMAX_SUMMARY)));
    }
}
