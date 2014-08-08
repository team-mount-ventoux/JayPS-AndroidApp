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
		Log.i(TAG, "onReceive: " + intent.getAction());
		process_intent(context, intent);
	}
	
	public static void process_intent(Context context, Intent intent) {
		Log.i(TAG, "process_intent");
		dump_bundle(intent.getExtras());

		GPSData data = new GPSData();
		
		data.distance = String.format("%.1f km", intent.getFloatExtra("DISTANCE", 0.0f) / 1000);
		data.altitude = String.format("%.0f m", intent.getDoubleExtra("ALTITUDE", 0));
		data.avgspeed = String.format("%.1f km/h", intent.getFloatExtra("AVGSPEED", 0) * 3.6);
		data.ascent = String.format("%.0f m", intent.getDoubleExtra("ASCENT", 0));
		data.bearing = String.format("%.0f m", intent.getFloatExtra("BEARING", 0.0f));
		data.time = String.format("%d s", intent.getLongExtra("TIME", 0));
		data.speed = String.format("%.1f km/h", intent.getFloatExtra("SPEED", 0) * 3.6);
		data.lat = String.format("%.3f", intent.getDoubleExtra("LAT", 0));
		data.lon = String.format("%.3f", intent.getDoubleExtra("LON", 0));
		data.ascentrate = String.format("%.0f m/h", intent.getFloatExtra("ASCENTRATE", 0) * 3600);
		data.slope = String.format("%.1f", intent.getFloatExtra("SLOPE", 0));
		data.accuracy = String.format("%.0f", intent.getFloatExtra("ACCURACY", 0));

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
