package com.njackson;

import com.njackson.CanvasPlugin.GPSData;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class CanvasPluginReceiver extends BroadcastReceiver {
	private static final String TAG = "PB-CanvasPluginReceiver";
	
	@Override
	public final void onReceive(Context context, Intent intent) {
	    if (MainActivity.debug) Log.i(TAG, "onReceive: " + intent.getAction());
	    if (!MainActivity.canvas_mode.equals("disable")) {
	        process_intent(context, intent);
	    }
	}
	
	public static void process_intent(Context context, Intent intent) {
		if (MainActivity.debug) dump_bundle(intent.getExtras());
		
		int units = Constants.METRIC;
        MainActivity activity = MainActivity.getInstance();
        if (activity == null) {
            return;
        }
        units = activity.getUnits();
        boolean display_units = activity.canvas_display_units;

		GPSData data = new GPSData();

		int time = (int) (intent.getLongExtra("TIME",0) / 1000);
        int s = time % 60;
        int m = ((time-s) / 60) % 60;
        int h = (time-s-60*m) / (60 * 60);
        float avgspeed = 0;
        if (time > 0) {
            avgspeed = intent.getFloatExtra("DISTANCE", 0.0f) / time;
        }

		data.distance = String.format("%.1f", intent.getFloatExtra("DISTANCE", 0.0f) * activity.distanceConversion);
		data.distance += display_units ? (units == Constants.METRIC ? " km" : " mi") : "";
		data.altitude = String.format("%.0f", intent.getDoubleExtra("ALTITUDE", 0) * activity.altitudeConversion);
		data.altitude += display_units ? (units == Constants.METRIC ? " m" : " ft") : "";
		data.avgspeed = String.format("%.1f", avgspeed * activity.speedConversion);
		data.avgspeed += display_units ? (units == Constants.METRIC ? " km/h" : " mph") : "";
		data.ascent = String.format("%.0f", intent.getDoubleExtra("ASCENT", 0) * activity.altitudeConversion);
		data.ascent += display_units ? (units == Constants.METRIC ? " m" : " ft") : "";
		data.bearing = String.format("%.0f", intent.getFloatExtra("BEARING", 0.0f));
		data.bearing += display_units ? "°" : "";
		if (display_units) {
		    data.time = String.format("%d:%02d:%02d", h, m, s);
		} else {
		    data.time = String.format("%d", time);
		}
		data.speed = String.format("%.1f", intent.getFloatExtra("SPEED", 0) * activity.speedConversion);
		data.speed += display_units ? (units == Constants.METRIC ? " km/h" : " mph") : "";
		data.lat = String.format("%.3f", intent.getDoubleExtra("LAT", 0));
		data.lon = String.format("%.3f", intent.getDoubleExtra("LON", 0));
		data.ascentrate = String.format("%.0f", intent.getFloatExtra("ASCENTRATE", 0)  * activity.altitudeConversion / 3600);
		data.ascentrate += display_units ? (units == Constants.METRIC ? " m/h" : " ft/h") : "";
		data.slope = String.format("%.1f", intent.getFloatExtra("SLOPE", 0));
		data.slope += display_units ? "%" : "";
		data.accuracy = String.format("%.0f", intent.getFloatExtra("ACCURACY", 0));
		data.accuracy += display_units ? "m" : "";
		data.heartrate = String.format("%d", intent.getIntExtra("HEARTRATE", 0));

		CanvasPlugin.set_gpsdata_details(data, context);
	}
	
	private static void dump_bundle(Bundle b) {
		if (b != null) {
			for (String key : b.keySet()) {
				Object o = b.get(key);
				if (o != null) {
					o.getClass().getName();
					Log.i(TAG, ".. extra: '" + key + "': '" + o + "' (" + o.getClass().getName() + ")");
				}
			}
		}
	}
}
