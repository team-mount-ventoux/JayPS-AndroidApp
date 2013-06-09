package com.njackson;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.maps.GoogleMap;

public class MapFragment extends com.njackson.SherlockMapFragment {
    private GoogleMap _map;
    private static View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        _map = getMap();
        _map.setMyLocationEnabled(true);
        //_map.se

        return root;
    }
}
