package com.njackson.test.application;

import android.test.ApplicationTestCase;

import com.njackson.analytics.IAnalytics;
import com.njackson.analytics.Parse;
import com.njackson.application.PebbleBikeApplication;
import com.njackson.application.modules.AndroidModule;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import static org.mockito.Mockito.mock;

/**
 * Created by njackson on 25/02/15.
 */
public class PebbleBikeApplicationTest extends ApplicationTestCase<TestApplication> {

    private TestApplication _app;

    @Inject IAnalytics _parseAnalytics;

    @Module(
            includes = AndroidModule.class,
            injects = PebbleBikeApplicationTest.class,
            overrides = true,
            complete = false
    )
    static class TestModule {
        @Provides
        @Singleton
        IAnalytics providesAnalytics() {
            return mock(IAnalytics.class);
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
    }
}
