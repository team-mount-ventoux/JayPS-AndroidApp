package com.njackson;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import com.actionbarsherlock.app.SherlockFragment;

/**
 * Created with IntelliJ IDEA.
 * User: njackson
 * Date: 05/06/2013
 * Time: 22:17
 * To change this template use File | Settings | File Templates.
 */
public class MapActivity extends SherlockFragment {
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return (RelativeLayout)inflater.inflate(R.layout.map, container, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MapFragment f = (MapFragment) getFragmentManager().findFragmentById(R.id.fragment_map);
        if (f != null)
            getFragmentManager().beginTransaction().remove(f).commit();
    }
}
