package com.njackson;

import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.model.LatLng;

public class MapFragment extends com.njackson.SherlockMapFragment {
	
	private static final String TAG = "PB-MapFragment";
	
    private GoogleMap _map;
    private static View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        _map = getMap();
        _map.setMyLocationEnabled(true);
        return root;
    }

    public void setLocation(LatLng location) {
        CameraUpdate center = CameraUpdateFactory.newLatLng(location);
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);
        _map.moveCamera(center);
        _map.animateCamera(zoom);
    }
}
