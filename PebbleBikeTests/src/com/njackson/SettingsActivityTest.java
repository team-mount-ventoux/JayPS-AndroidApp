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

    public SettingsActivityTest() {
        super(SettingsActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        //setActivityInitialTouchMode(false);
        Context targetContext = getInstrumentation().getTargetContext();
        intent = new Intent(targetContext, SettingsActivity.class);
        startActivity(intent, null, null);

        mActivity = getActivity();
        assertNotNull(mActivity);

        installPreference = mActivity.findPreference("pref_install");
        assertNotNull(installPreference);
    }

    public void testInstallText() {
        assertEquals("Some stuff",installPreference.getTitle());
    }

}
