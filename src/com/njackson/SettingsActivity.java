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
import android.preference.Preference;
import android.preference.PreferenceActivity;
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

        Preference pref = findPreference("pref_install");
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                // TODO Auto-generated method stub
            	Log.d(TAG, "onPreferenceClick:" + preference.getKey());
            	if (preference.getKey().equals("pref_install")) {
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
            		
            		try {
	                    Uri uri = Uri.parse("http://labs.jayps.fr/pebblebike/pebblebike-1.3.0-beta1.pbw?and&v=" + versionCode);
	                    Intent startupIntent = new Intent();
	                    startupIntent.setAction(Intent.ACTION_VIEW);
	                    startupIntent.setType("application/octet-stream");
	                    startupIntent.setData(uri);
	                    ComponentName distantActivity = new ComponentName("com.getpebble.android", "com.getpebble.android.ui.UpdateActivity");
	                    startupIntent.setComponent(distantActivity);
	                    startActivity(startupIntent);
	                } catch (ActivityNotFoundException ae) {
	                    Toast.makeText(getApplicationContext(),"Unable to install watchface, do you have the latest pebble app installed?",Toast.LENGTH_LONG).show();
	                }
            	}
            	return false;
            }
        });

    }
	
	@Override
    protected void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        MainActivity activity = MainActivity.getInstance();
        if(activity != null)
            activity.loadPreferences(sharedPreferences);
    }

}
