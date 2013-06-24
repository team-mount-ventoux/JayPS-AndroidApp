package com.njackson;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import com.actionbarsherlock.app.SherlockFragment;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created with IntelliJ IDEA.
 * User: njackson
 * Date: 05/06/2013
 * Time: 22:17
 * To change this template use File | Settings | File Templates.
 */
public class MapActivity extends SherlockFragment{

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return (RelativeLayout)inflater.inflate(R.layout.map, container, false);
    }

    public void setLocation(LatLng location) {
        try {

        MapFragment map = (MapFragment)getFragmentManager().findFragmentById(R.id.fragment_map);
        if(map != null)
            map.setLocation(location);
        }catch(NullPointerException ex) {
            Log.d("MainActivity","Map does not exist");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MapFragment f = (MapFragment) getFragmentManager().findFragmentById(R.id.fragment_map);
        if (f != null)
            getFragmentManager().beginTransaction().remove(f).commit();
    }
}
