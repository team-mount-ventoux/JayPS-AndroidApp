package com.njackson;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.test.ActivityUnitTestCase;

/**
 * Created by njackson on 12/01/2014.
 */
public class SettingsActivityTest extends ActivityUnitTestCase<SettingsActivity> {

    private SettingsActivity mActivity;
    private Intent intent;
    private Preference installPreference;
    private Context targetContext;

    public SettingsActivityTest() {
        super(SettingsActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        //setActivityInitialTouchMode(false);
        targetContext = getInstrumentation().getTargetContext();
        intent = new Intent(targetContext, SettingsActivity.class);
        startActivity(intent, null, null);

        mActivity = getActivity();
        assertNotNull(mActivity);

        installPreference = mActivity.findPreference("pref_install");
        assertNotNull(installPreference);
    }

    public void testInstallText() {
        String title = targetContext.getString(R.string.PREF_INSTALL_WATCHFACE);
        assertEquals(
                 title + " " + MainActivity.pebbleFirmwareVersion + ".x",
                installPreference.getTitle());
    }

    public void testInstallSummary() {
        String summary = targetContext.getString(R.string.PREF_INSTALL_WATCHFACE_SUMMARY);
        assertEquals(
                summary + " " + MainActivity.pebbleFirmwareVersion + ".x only",
                installPreference.getSummary());
    }

    public void testInstallListener() {
        Preference.OnPreferenceClickListener onPreferenceClickListener = installPreference.getOnPreferenceClickListener();
        assertNotNull(onPreferenceClickListener);
    }

}
