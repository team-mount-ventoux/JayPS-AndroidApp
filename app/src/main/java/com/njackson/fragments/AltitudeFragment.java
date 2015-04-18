package com.njackson.fragments;

import android.graphics.drawable.ClipDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.njackson.R;
import com.njackson.events.GPSServiceCommand.NewAltitude;
import com.njackson.events.GPSServiceCommand.NewLocation;
import com.njackson.events.GPSServiceCommand.ResetGPSState;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: njackson
 * Date: 31/07/2013
 * Time: 20:59
 * To change this template use File | Settings | File Templates.
 */
public class AltitudeFragment extends BaseFragment {
	
	private static final String TAG = "PB-AltitudeFragment";

    private float[] _prevValues;
    private View _view;

    public AltitudeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreateView(inflater,container,savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_altitude, container, false);
        _prevValues = new float[] {1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f};
        _view = view;
        return view;
    }

    @Subscribe
    public void onNewAltitudeEvent(NewAltitude event) {
        setAltitude(event.getAltitudes(), true);
    }

    @Subscribe
    public void onResetGPSStateEvent(ResetGPSState event) {
        // _altitudeGraphReduce is reseted in GPSServiceCommand, we only reset the display here
        setAltitude(new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0}, true);
    }
    @Subscribe
    public void onNewLocation(NewLocation event) {
        setGPSStatus(event.getAccuracy());

    }

    private void setGPSStatus(float accuracy) {
        String gpsStatus = "";
        int level = 0;
        if (accuracy == 0) {
            gpsStatus = getString(R.string.altitude_status_disable);
            level = 0;
        } else if (accuracy <= 4) {
            gpsStatus = getString(R.string.altitude_status_excellent);
            level = 10000;
        } else if (accuracy <= 6) {
            gpsStatus = getString(R.string.altitude_status_good);
            level = 7200;
        } else if (accuracy <= 10) {
            gpsStatus = getString(R.string.altitude_status_medium);
            level = 5000;
        } else {
            gpsStatus = getString(R.string.altitude_status_poor);
            level = 2500;
        }

        TextView altitudeStatus = (TextView)getActivity().findViewById(R.id.altitude_status_value);
        altitudeStatus.setText(gpsStatus);

        // mask a part of the image (orientation horizontal, gravity left, 0 to 10000)
        ImageView img = (ImageView) getActivity().findViewById(R.id.altitude_graph);
        ClipDrawable mImageDrawable = (ClipDrawable) img.getDrawable();
        mImageDrawable.setLevel(level);
    }

    // sets the bars in the animation so the given values
    // final height is a percentage of the parent container height based on value[n] / maxValue
    public void setAltitude(int[] values, boolean animate){

        // get a list of the imageviews
        ImageView[] views = findAltitudeBars();
        float[] displayValues = calculatePercentages(values, views);
        animateAltitudeBars(views, displayValues);
    }

    private void animateAltitudeBars(ImageView[] views, float[] displayValues) {

        for(int v =0; v < views.length;v++) {
            views[v].setMaxHeight(_view.getHeight());
            ScaleAnimation scale = new ScaleAnimation(1, 1, _prevValues[v], displayValues[v], Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 1.0f);
            scale.setFillAfter(true);
            scale.setDuration(500);
            scale.setInterpolator(new AccelerateInterpolator(1.0f));
            views[v].startAnimation(scale);
        }

        _prevValues = displayValues;

    }

    private float[] calculatePercentages(int[] values, ImageView[] views) {

        float[] percentages = new float[values.length];
        // parent container is too big or not fully displayed ?
        int frameHeight = _view.getHeight() / 2;

        int minValue = Integer.MAX_VALUE;
        int maxValue = Integer.MIN_VALUE;
        for(int v = 0; v < values.length;v++) {
            if (values[v] != 0) {
                // 0: not yet used
                if (values[v] > maxValue) {
                    maxValue = values[v];
                }
                if (values[v] < minValue) {
                    minValue = values[v];
                }
            }
        }
/*
        String s = "";
        for(int v = 0; v < values.length;v++) {
            s += " " + values[v];
        }
        Log.d(TAG, "calculatePercentages " + s + " //" + minValue + " " + maxValue);
*/
        for(int v = 0; v < values.length;v++) {
            float currentHeight = views[v].getHeight();
            if (values[v] != 0) {
                float sizePercentage = maxValue != minValue ? ((float) (values[v] - minValue) / (float) (maxValue - minValue)) : 0.5f;
                float newHeight = sizePercentage * frameHeight;
                if (newHeight < currentHeight) {
                    newHeight = currentHeight;
                }

                percentages[v] = newHeight / currentHeight;

                //Log.d(TAG, "views["+v+"]:" + views[v].getHeight() + "sizePercent:" + sizePercentage+ ", newHeight:" + newHeight + ",percent:" + percentages[v]);
            } else {
                // 0: not yet used
                percentages[v] = 1;
            }
        }
/*
        s = "";
        for(int v = 0; v < values.length;v++) {
            s += " " + ((int)percentages[v]);
        }
        Log.d(TAG, "calculatePercentages  " + s);
*/
        return percentages;

    }

    private ImageView[] findAltitudeBars() {

        ArrayList<ImageView> altitudeViews = new ArrayList<ImageView>();
        LinearLayout layout = (LinearLayout)_view.findViewById(R.id.altitude_main_container);

        for(int c=0; c< layout.getChildCount(); c++) {

            View tempView = layout.getChildAt(c);
            if(tempView.getClass().getName() == ImageView.class.getName()) {
                altitudeViews.add((ImageView)tempView);
            }

        }

        return altitudeViews.toArray(new ImageView[altitudeViews.size()]);

    }
}