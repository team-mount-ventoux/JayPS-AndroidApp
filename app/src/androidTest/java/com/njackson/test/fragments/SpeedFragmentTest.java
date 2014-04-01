package com.njackson.test.fragments;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.MediumTest;
import android.widget.TextView;

import com.njackson.R;
import com.njackson.application.ForApplication;
import com.njackson.application.PebbleBikeModule;
import com.njackson.events.GPSService.NewLocationEvent;
import com.njackson.fragments.SpeedFragment;
import com.njackson.test.application.TestApplication;
import com.squareup.otto.Bus;

import org.mockito.Mockito;

import javax.inject.Inject;

import dagger.Module;
import dagger.ObjectGraph;

/**
 * Created by server on 30/03/2014.
 */
public class SpeedFragmentTest extends ActivityInstrumentationTestCase2<SpeedFragment> {

    @Inject Bus _bus;

    private SpeedFragment _activity;

    private TextView _speedLabel;
    private TextView _speedText;
    private TextView _speedUnitsLabel;

    private TextView _timeLabel;
    private TextView _timeText;

    private TextView _distanceLabel;
    private TextView _distanceText;
    private TextView _distanceUnitsLabel;

    private TextView _avgspeedLabel;
    private TextView _avgspeedText;
    private TextView _avgspeedUnitsLabel;
    private SharedPreferences _mock;

    public SpeedFragmentTest() {
        super(SpeedFragment.class);
    }

    @Module(
            includes = PebbleBikeModule.class,
            injects = SpeedFragmentTest.class,
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

        setupMocks();
        this.getInstrumentation().waitForIdleSync(); // this is needed for emulator versions 2.3 as the application is instantiated on a separate thread.
        TestApplication app = (TestApplication)this.getInstrumentation().getTargetContext().getApplicationContext();

        app.setObjectGraph(ObjectGraph.create(new TestModule()));
        app.inject(this);

        setActivityInitialTouchMode(false);

        _activity = getActivity();

        _speedLabel = (TextView)_activity.findViewById(R.id.speed_label);
        _speedText = (TextView)_activity.findViewById(R.id.speed_text);
        _speedUnitsLabel = (TextView)_activity.findViewById(R.id.speed_units_label);

        _timeLabel = (TextView)_activity.findViewById(R.id.time_label);
        _timeText = (TextView)_activity.findViewById(R.id.time_text);

        _distanceLabel = (TextView)_activity.findViewById(R.id.distance_label);
        _distanceText = (TextView)_activity.findViewById(R.id.distance_text);
        _distanceUnitsLabel = (TextView)_activity.findViewById(R.id.distance_units_label);

        _avgspeedLabel = (TextView)_activity.findViewById(R.id.avgspeed_label);
        _avgspeedText = (TextView)_activity.findViewById(R.id.avgspeed_text);
        _avgspeedUnitsLabel = (TextView)_activity.findViewById(R.id.avgspeed_units_label);
    }

    private void setupMocks() {
        //Configure Mokito
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());
        _mock = Mockito.mock(SharedPreferences.class);
    }

    @MediumTest
    public void testElementsExist() {
        assertNotNull(_speedLabel);
        assertNotNull(_speedText);
        assertNotNull(_speedUnitsLabel);

        assertNotNull(_timeLabel);
        assertNotNull(_timeText);

        assertNotNull(_distanceLabel);
        assertNotNull(_distanceText);
        assertNotNull(_distanceUnitsLabel);

        assertNotNull(_avgspeedLabel);
        assertNotNull(_avgspeedText);
        assertNotNull(_avgspeedUnitsLabel);
    }

    @MediumTest
    public void testRespondsToNewLocationEvent() throws InterruptedException {
        final NewLocationEvent event = new NewLocationEvent();
        event.setSpeed(20.0f);

        _activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _bus.post(event);
            }
        });

        Thread.sleep(100);

        assertEquals("20.0", _speedText.getText());
    }

}