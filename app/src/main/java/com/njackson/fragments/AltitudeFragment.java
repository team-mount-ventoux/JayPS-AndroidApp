package com.njackson.fragments;

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

import com.njackson.R;
import com.njackson.events.GPSServiceCommand.NewAltitude;
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
        setAltitude(_prevValues,1,true);
    }

    // sets the bars in the animation so the given values
    // final height is a percentage of the parent container height based on value[n] / maxValue
    public void setAltitude(float[] values, int maxValue, boolean animate){

        // get a list of the imageviews
        ImageView[] views = findAltitudeBars();
        float[] displayValues = calculatePercentages(values, maxValue,views);
        animateAltitudeBars(views,displayValues);

        Log.d(TAG,"Height:" + _view.getHeight());
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

    private float[] calculatePercentages(float[] values, int maxValue,ImageView[] views) {

        float[] percentages = new float[values.length];
        int frameHeight = _view.getHeight();
        for(int v = 0; v < values.length;v++) {

            float currentHeight = views[v].getHeight();
            float sizePercentage = ((float)values[v] / (float)maxValue);
            float newHeight =  sizePercentage* frameHeight;
            percentages[v] = newHeight / currentHeight;

        }

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