package com.njackson.test.application;

import android.content.SharedPreferences;
import android.test.ApplicationTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.analytics.IAnalytics;
import com.njackson.application.modules.AndroidModule;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by njackson on 25/02/15.
 */
public class PebbleBikeApplicationTest extends ApplicationTestCase<TestApplication> {

    private TestApplication _app;

    @Inject IAnalytics _parseAnalytics;
    @Inject SharedPreferences _mockPreferences;

    @Module(
            includes = AndroidModule.class,
            injects = {PebbleBikeApplicationTest.class, TestApplication.class},
            overrides = true,
            complete = false
    )
    static class TestModule {
        @Provides
        @Singleton
        IAnalytics providesAnalytics() {
            return mock(IAnalytics.class);
        }

        @Provides
        @Singleton
        SharedPreferences provideSharedPreferences() {
            return mock(SharedPreferences.class);
        }
    }

    public PebbleBikeApplicationTest() {
        super(TestApplication.class);
    }

    public PebbleBikeApplicationTest(Class<TestApplication> applicationClass) {
        super(applicationClass);
    }

    public void setUp() throws Exception {
        super.setUp();

        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());

        createApplication();
        _app  = getApplication();
        _app.setObjectGraph(ObjectGraph.create(TestModule.class));
        _app.inject(this);

        setupMocks();
    }
    private void setupMocks() {
        when(_mockPreferences.getBoolean("PREF_REPORT_CRASH", false)).thenReturn(true);
    }

    @SmallTest
    public void testEnablesAnalytics() {
        _app.setupAnalytics();
        verify(_parseAnalytics,times(1)).enable(_app);
    }

    @SmallTest
    public void testOnLowMemoryCallsAnalytics() {
        _app.setupAnalytics();
        _app.onLowMemory();

        verify(_parseAnalytics,times(1)).trackLowMemory();
    }
}
