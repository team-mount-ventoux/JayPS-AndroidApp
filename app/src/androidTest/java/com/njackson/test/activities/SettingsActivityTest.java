package com.njackson.test.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.test.ActivityUnitTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.application.SettingsActivity;
import com.njackson.application.modules.PebbleBikeModule;
import com.njackson.test.application.TestApplication;
import com.njackson.utils.IInstallWatchFace;
import com.njackson.utils.IMessageMaker;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

    @Inject IInstallWatchFace _watchFaceMock;

    @Module(
            includes = PebbleBikeModule.class,
            injects = SettingsActivityTest.class,
            overrides = true,
            complete = false
    )
    static class TestModule {
        @Provides @Singleton
        IInstallWatchFace providesWatchFaceInstall() {
            return mock(IInstallWatchFace.class);
        }

        @Provides
        @Singleton
        SharedPreferences provideSharedPreferences() {
            return mock(SharedPreferences.class);
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        TestApplication app = new TestApplication();
        app.setObjectGraph(ObjectGraph.create(TestModule.class));
        app.inject(this);
        setApplication(app);

        setupMocks();

        //setActivityInitialTouchMode(false);
        _targetContext = getInstrumentation().getTargetContext();
        Intent intent = new Intent(_targetContext, SettingsActivity.class);
        startActivity(intent, null, null);

        _activity = getActivity();
        assertNotNull(_activity);

        _installPreference = _activity.findPreference("pref_install");
        assertNotNull(_installPreference);
    }

    private void setupMocks() {

    }

    @SmallTest
    public void testInstallListener() {
        Preference.OnPreferenceClickListener onPreferenceClickListener = _installPreference.getOnPreferenceClickListener();
        assertNotNull(onPreferenceClickListener);
    }

    @SmallTest
    public void testOnPreferenceClickExecutesInstallWatchFace(){
        Preference.OnPreferenceClickListener onPreferenceClickListener = _installPreference.getOnPreferenceClickListener();
        boolean b = onPreferenceClickListener.onPreferenceClick(new Preference(_targetContext));

        verify(_watchFaceMock, times(1)).execute(any(Context.class), any(IMessageMaker.class));
    }
}
