package com.njackson;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: njackson
 * Date: 31/07/2013
 * Time: 20:59
 * To change this template use File | Settings | File Templates.
 */
public class AltitudeFragment extends Fragment {

    private float[] _prevValues;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = (LinearLayout)inflater.inflate(R.layout.altitudefragment, container, false);
        _prevValues = new float[] {1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f};
        return view;

    }

    // sets the bars in the animation so the given values
    // final height is a percentage of the parent container height based on value[n] / maxValue
    public void setAltitude(int[] values, int maxValue, boolean animate){

        // get a list of the imageviews
        ImageView[] views = findAltitudeBars();
        float[] displayValues = caluclatePercentages(values, maxValue,views);
        animateAltitudeBars(views,displayValues);

    }

    private void animateAltitudeBars(ImageView[] views, float[] displayValues) {

        for(int v =0; v < views.length;v++) {
            views[v].setMaxHeight(getView().getHeight());
            ScaleAnimation scale = new ScaleAnimation(1, 1, _prevValues[v], displayValues[v], Animation.RELATIVE_TO_SELF, 1.0f, Animation.RELATIVE_TO_SELF, 1.0f);
            scale.setFillAfter(true);
            scale.setDuration(500);
            scale.setInterpolator(new AccelerateInterpolator(1.0f));
            views[v].startAnimation(scale);
        }

        _prevValues = displayValues;

    }

    private float[] caluclatePercentages(int[] values, int maxValue,ImageView[] views) {

        float[] percentages = new float[values.length];
        int frameHeight = getView().getHeight();
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
        LinearLayout layout = (LinearLayout)getView().findViewById(R.id.ALT_MAIN_CONTAINER);

        for(int c=0; c< layout.getChildCount(); c++) {

            View tempView = layout.getChildAt(c);
            if(tempView.getClass().getName() == ImageView.class.getName()) {
                altitudeViews.add((ImageView)tempView);
            }

        }

        return altitudeViews.toArray(new ImageView[altitudeViews.size()]);

    }
}