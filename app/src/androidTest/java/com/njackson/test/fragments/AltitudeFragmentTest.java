package com.njackson.test.fragments;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.MediumTest;
import android.widget.LinearLayout;

import com.njackson.R;
import com.njackson.activities.TestActivity;
import com.njackson.application.PebbleBikeModule;
import com.njackson.events.GPSService.NewAltitiudeEvent;
import com.njackson.fragments.AltitudeFragment;
import com.njackson.fragments.SpeedFragment;
import com.njackson.test.FragmentInstrumentTestCase2;
import com.njackson.test.application.TestApplication;
import com.squareup.otto.Bus;

import javax.inject.Inject;

import dagger.Module;
import dagger.ObjectGraph;

/**
 * Created by server on 04/04/2014.
 */
public class AltitudeFragmentTest extends FragmentInstrumentTestCase2 {

    @Inject Bus _bus;

    @Module(
            includes = PebbleBikeModule.class,
            injects = AltitudeFragmentTest.class,
            overrides = true
    )
    static class TestModule {
        /*
        @Provides @Singleton Heater provideHeater() {
            return Mockito.mock(Heater.class);
        }
        */
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.getInstrumentation().waitForIdleSync(); // this is needed for emulator versions 2.3 as the application is instantiated on a separate thread.
        TestApplication app = (TestApplication)this.getInstrumentation().getTargetContext().getApplicationContext();

        app.setObjectGraph(ObjectGraph.create(new TestModule()));
        app.inject(this);

        setActivityInitialTouchMode(false);

        _activity = getActivity();
        startFragment(new AltitudeFragment());
    }

    @MediumTest
    public void test_Element_Exists() {
        LinearLayout layout = (LinearLayout)_activity.findViewById(R.id.altitude_main_container);
        int elements = layout.getChildCount();

        assertEquals("Expected 14 altitude bars",14,elements);
    }

    @MediumTest
    public void test_Activity_Responds_To_NewAltitudeEvent() {

        final NewAltitiudeEvent event = new NewAltitiudeEvent(new float[14],0);

        _activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _bus.post(event);
            }
        });

    }

}
