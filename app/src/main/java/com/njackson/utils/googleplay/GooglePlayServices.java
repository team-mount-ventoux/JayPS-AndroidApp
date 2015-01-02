package com.njackson.utils.googleplay;

import android.content.Context;

import com.google.android.gms.common.GooglePlayServicesUtil;

/**
 * Created by njackson on 02/01/15.
 */
public class GooglePlayServices implements IGooglePlayServices {
    @Override
    public int isGooglePlayServicesAvailable(Context context) {
        return GooglePlayServicesUtil.
                isGooglePlayServicesAvailable(context);
    }
}
