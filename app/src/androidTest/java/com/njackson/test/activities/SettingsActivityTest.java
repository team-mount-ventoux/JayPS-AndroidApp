package com.njackson.test.activities;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.test.ActivityUnitTestCase;

import com.njackson.application.SettingsActivity;

/**
 * Created by server on 28/06/2014.
 */
public class SettingsActivityTest extends ActivityUnitTestCase<SettingsActivity> {
    private Context _targetContext;
    private SettingsActivity _activity;
    private Preference _installPreference;

    public SettingsActivityTest() {
        super(SettingsActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        //setActivityInitialTouchMode(false);
        _targetContext = getInstrumentation().getTargetContext();
        Intent intent = new Intent(_targetContext, SettingsActivity.class);
        startActivity(intent, null, null);

        _activity = getActivity();
        assertNotNull(_activity);

        _installPreference = _activity.findPreference("pref_install");
        assertNotNull(_installPreference);
    }

    public void testInstallListener() {
        Preference.OnPreferenceClickListener onPreferenceClickListener = _installPreference.getOnPreferenceClickListener();
        assertNotNull(onPreferenceClickListener);
    }
}
