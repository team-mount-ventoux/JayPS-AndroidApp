package com.pebblebike.fragments;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.MediumTest;
import android.widget.TextView;

import com.pebblebike.R;

import org.w3c.dom.Text;

/**
 * Created by server on 30/03/2014.
 */
public class SpeedFragmentTest extends ActivityInstrumentationTestCase2<SpeedFragment> {

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





    public SpeedFragmentTest() {
        super(SpeedFragment.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

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
        assertEquals(true,false);
    }

}