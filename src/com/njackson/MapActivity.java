package com.njackson;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        return inflater.inflate(R.layout.map, container, false);
    }
}
