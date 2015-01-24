package com.njackson.adapters;


import com.njackson.Constants;
import com.njackson.events.GPSServiceCommand.NewLocation;
import com.njackson.pebble.canvas.GPSData;

public class NewLocationToCanvasPluginGPSData extends GPSData {

    private static final String TAG = "PB-NewLocationToCanvasPluginGPSData";

    public NewLocationToCanvasPluginGPSData(NewLocation event, boolean display_units) {

        int time = (int) (event.getElapsedTimeSeconds());
        int s = time % 60;
        int m = ((time-s) / 60) % 60;
        int h = (time-s-60*m) / (60 * 60);

        this.distance = String.format("%.1f", event.getDistance());
        this.distance += display_units ? (event.getUnits() == Constants.METRIC ? " km" : " mi") : "";
        this.altitude = String.format("%.0f", event.getAltitude());
        this.altitude += display_units ? (event.getUnits() == Constants.METRIC ? " m" : " ft") : "";
        this.avgspeed = String.format("%.1f", event.getAverageSpeed());
        this.avgspeed += display_units ? (event.getUnits() == Constants.METRIC ? " km/h" : " mph") : "";
        this.ascent = String.format("%.0f", event.getAscent());
        this.ascent += display_units ? (event.getUnits() == Constants.METRIC ? " m" : " ft") : "";
        this.bearing = String.format("%.0f", event.getBearing());
        this.bearing += display_units ? "°" : "";
        if (display_units) {
            this.time = String.format("%d:%02d:%02d", h, m, s);
        } else {
            this.time = String.format("%d", time);
        }
        this.speed = String.format("%.1f", event.getSpeed());
        this.speed += display_units ? (event.getUnits() == Constants.METRIC ? " km/h" : " mph") : "";
        this.lat = String.format("%.3f", event.getLatitude());
        this.lon = String.format("%.3f", event.getLongitude());
        this.ascentrate = String.format("%.0f", event.getAscentRate() / 3600);
        this.ascentrate += display_units ? (event.getUnits() == Constants.METRIC ? " m/h" : " ft/h") : "";
        this.slope = String.format("%.1f", event.getSlope());
        this.slope += display_units ? "%" : "";
        this.accuracy = String.format("%.0f", event.getAccuracy());
        this.accuracy += display_units ? "m" : "";
        this.heartrate = String.format("%d", event.getHeartRate());
    }
}
