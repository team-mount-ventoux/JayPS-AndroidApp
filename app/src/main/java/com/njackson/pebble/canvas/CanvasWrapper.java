package com.njackson.pebble.canvas;

import android.content.Context;

/**
 * Created by njackson on 24/01/15.
 */
public class CanvasWrapper implements ICanvasWrapper {
    @Override
    public void set_gpsdata_details(GPSData data, Context context) {
        CanvasPlugin.set_gpsdata_details(data, context);
    }
}
