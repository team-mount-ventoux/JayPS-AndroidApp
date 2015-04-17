package com.njackson.test.fragments;

import android.content.SharedPreferences;
import android.location.LocationManager;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.widget.LinearLayout;

import com.njackson.R;
import com.njackson.application.modules.AndroidModule;
import com.njackson.events.GPSServiceCommand.NewAltitude;
import com.njackson.fragments.AltitudeFragment;
import com.njackson.test.FragmentInstrumentTestCase2;
import com.njackson.test.application.TestApplication;
import com.squareup.otto.Bus;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import static org.mockito.Mockito.mock;

/**
 * Created by server on 04/04/2014.
 */
public class AltitudeFragmentTest extends FragmentInstrumentTestCase2 {

    @Inject Bus _bus;
    private LinearLayout _mainContainer;

    @Module(
            includes = AndroidModule.class,
            injects = AltitudeFragmentTest.class,
            overrides = true,
            complete = false
    )
    static class TestModule {
        @Provides
        @Singleton
        LocationManager provideLocationManager() {
            return mock(LocationManager.class);
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

        TestApplication app = (TestApplication)this.getInstrumentation().getTargetContext().getApplicationContext();

        app.setObjectGraph(ObjectGraph.create(new TestModule()));
        app.inject(this);

        setActivityInitialTouchMode(false);

        _activity = getActivity();
        startFragment(new AltitudeFragment());

        _mainContainer = (LinearLayout)_activity.findViewById(R.id.altitude_main_container);
        assertNotNull(_mainContainer);
    }

    @SmallTest
    public void test_Element_Exists() {
        int elements = _mainContainer.getChildCount();

        assertEquals("Expected 14 altitude bars",14,elements);
    }

    @MediumTest
    public void test_Activity_Responds_To_NewAltitudeEvent() {
        _bus.post(new NewAltitude(new int[14]));

    }

}
