package com.njackson.test.fragments;

import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.test.UiThreadTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;
import android.widget.TextView;

import com.njackson.R;
import com.njackson.application.modules.AndroidModule;
import com.njackson.events.GPSServiceCommand.NewLocation;
import com.njackson.fragments.SpeedFragment;
import com.njackson.test.FragmentInstrumentTestCase2;
import com.njackson.test.application.TestApplication;
import com.squareup.otto.Bus;

import org.mockito.Mockito;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by server on 30/03/2014.
 */
public class SpeedFragmentTest extends FragmentInstrumentTestCase2 {

    @Inject Bus _bus;
    @Inject SharedPreferences _mockPreferences;

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
    private SharedPreferences.Editor _mockEditor;

    @Module(
            includes = AndroidModule.class,
            injects = SpeedFragmentTest.class,
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

        this.getInstrumentation().waitForIdleSync(); // this is needed for emulator versions 2.3 as the application is instantiated on a separate thread.
        TestApplication app = (TestApplication)this.getInstrumentation().getTargetContext().getApplicationContext();

        app.setObjectGraph(ObjectGraph.create(new TestModule()));
        app.inject(this);

        setupMocks();

        setActivityInitialTouchMode(false);

        _activity = getActivity();

        startFragment(new SpeedFragment());

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

        _mockEditor = mock(SharedPreferences.Editor.class);
        when(_mockPreferences.edit()).thenReturn(_mockEditor);

        when(_mockPreferences.getString("SPEEDFRAGMENT_SPEED", getInstrumentation().getTargetContext().getString(R.string.speedfragment_speed_value))).thenReturn("1.1");
        when(_mockPreferences.getString("SPEEDFRAGMENT_AVGSPEED", getInstrumentation().getTargetContext().getString(R.string.speedfragment_avgspeed_value))).thenReturn("1.2");
        when(_mockPreferences.getString("SPEEDFRAGMENT_DISTANCE", getInstrumentation().getTargetContext().getString(R.string.speedfragment_distance_value))).thenReturn("1.3");
        when(_mockPreferences.getString("SPEEDFRAGMENT_TIME", getInstrumentation().getTargetContext().getString(R.string.speedfragment_time_value))).thenReturn("1.4");
    }

    @SmallTest
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

    @SmallTest
    @UiThreadTest
    public void testLoadsInstanceStateSpeed() {
        String text = _speedText.getText().toString();

        // when the app is started, the instant speed is force to default value (0.0)
        assertEquals(_activity.getString(R.string.speedfragment_speed_value),text);
    }

    @SmallTest
    @UiThreadTest
    public void testLoadsInstanceStateAvgSpeed() {
        String text = _avgspeedText.getText().toString();

        assertEquals("1.2",text);
    }

    @SmallTest
    @UiThreadTest
    public void testLoadsInstanceStateDistanceSpeed() {
        String text = _distanceText.getText().toString();

        assertEquals("1.3",text);
    }

    @SmallTest
    @UiThreadTest
    public void testLoadsInstanceStateTime() {
        String text = _timeText.getText().toString();

        assertEquals("1.4",text);
    }

    @SmallTest
    @UiThreadTest
    public void testSavesInstanceStateCallsCommitAndEdit() {
        Bundle mockOutState = new Bundle();
        getInstrumentation().callActivityOnSaveInstanceState(_activity,mockOutState);

        verify(_mockPreferences,times(1)).edit();
        verify(_mockEditor,times(1)).commit();
    }

    @SmallTest
    @UiThreadTest
    public void testSavesInstanceStateSpeed() {
        String speed = _speedText.getText().toString();

        Bundle mockOutState = new Bundle();
        getInstrumentation().callActivityOnSaveInstanceState(_activity,mockOutState);

        verify(_mockEditor,times(1)).putString("SPEEDFRAGMENT_SPEED",speed);
    }

    @SmallTest
    @UiThreadTest
    public void testSavesInstanceStateAvgSpeed() {
        String avgspeed = _avgspeedText.getText().toString();

        Bundle mockOutState = new Bundle();
        getInstrumentation().callActivityOnSaveInstanceState(_activity,mockOutState);

        verify(_mockEditor,times(1)).putString("SPEEDFRAGMENT_AVGSPEED",avgspeed);
    }

    @SmallTest
    @UiThreadTest
    public void testSavesInstanceStateDistance() {
        String distance = _distanceText.getText().toString();

        Bundle mockOutState = new Bundle();
        getInstrumentation().callActivityOnSaveInstanceState(_activity,mockOutState);

        verify(_mockEditor,times(1)).putString("SPEEDFRAGMENT_DISTANCE",distance);
    }

    @SmallTest
    @UiThreadTest
    public void testSavesInstanceStateTime() {
        String time = _timeText.getText().toString();

        Bundle mockOutState = new Bundle();
        getInstrumentation().callActivityOnSaveInstanceState(_activity,mockOutState);

        verify(_mockEditor,times(1)).putString("SPEEDFRAGMENT_TIME",time);
    }

    @SmallTest
    public void testRespondsToNewLocationEvent() throws InterruptedException {
        final NewLocation event = new NewLocation();
        event.setSpeed(20.0f);
        event.setDistance(100.0f);
        event.setAvgSpeed(25.4f);
        event.setElapsedTimeSeconds((3600 + 240 + 34)); // "1:04:34"

        _bus.post(event);
        Thread.sleep(200);

        assertEquals("20.0", _speedText.getText());
        assertEquals("100.0", _distanceText.getText());
        assertEquals("25.4", _avgspeedText.getText());
        assertEquals("1:04:34", _timeText.getText());
    }

    @SmallTest
    public void testFormatsDistanceTo1DP() throws InterruptedException {
        final NewLocation event = new NewLocation();
        event.setDistance(24.1234f);

        _bus.post(event);
        Thread.sleep(200);

        assertEquals("24.1", _distanceText.getText());
    }

    @SmallTest
    public void testFormatsAvgSpeedTo1DP() throws InterruptedException {
        final NewLocation event = new NewLocation();
        event.setAvgSpeed(29.1234f);

        _bus.post(event);
        Thread.sleep(200);

        assertEquals("29.1", _avgspeedText.getText());
    }

    @SmallTest
    public void testFormatsSpeedTo1DP() throws InterruptedException {
        final NewLocation event = new NewLocation();
        event.setSpeed(20.1234f);

        _bus.post(event);
        Thread.sleep(200);

        assertEquals("20.1", _speedText.getText());
    }

}