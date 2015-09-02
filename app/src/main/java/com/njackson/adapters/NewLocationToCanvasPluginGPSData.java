package com.njackson.adapters;


import android.util.Log;

import com.njackson.Constants;
import com.njackson.events.GPSServiceCommand.NewLocation;
import com.njackson.pebble.canvas.GPSData;
import com.njackson.utils.NumberConverter;
import com.njackson.utils.Units;

public class NewLocationToCanvasPluginGPSData extends GPSData {

    private static final String TAG = "PB-NewLocationToCanvasPluginGPSData";

    public NewLocationToCanvasPluginGPSData(NewLocation event, boolean display_units) {

        NumberConverter converter = new NumberConverter();

        int time = (int) (event.getElapsedTimeSeconds());
        int s = time % 60;
        int m = ((time-s) / 60) % 60;
        int h = (time-s-60*m) / (60 * 60);

        this.distance = String.format("%.1f", event.getDistance());
        this.distance += display_units ? Units.getDistanceUnits(event.getUnits()) : "";
        this.altitude = String.format("%.0f", event.getAltitude());
        this.altitude += display_units ? Units.getAltitudeUnits(event.getUnits()) : "";
        if (Units.isPace(event.getUnits())) {
            this.avgspeed = converter.convertSpeedToPace(event.getAverageSpeed());
        } else {
            this.avgspeed = String.format("%.1f", event.getAverageSpeed());
        }
        this.avgspeed += display_units ? Units.getSpeedUnits(event.getUnits()) : "";
        this.ascent = String.format("%.0f", event.getAscent());
        this.ascent += display_units ? Units.getAltitudeUnits(event.getUnits()) : "";
        this.bearing = String.format("%.0f", event.getBearing());
        this.bearing += display_units ? "°" : "";
        if (display_units) {
            this.time = String.format("%d:%02d:%02d", h, m, s);
        } else {
            this.time = String.format("%d", time);
        }
        if (Units.isPace(event.getUnits())) {
            this.speed = converter.convertSpeedToPace(event.getSpeed());
        } else {
            this.speed = String.format("%.1f", event.getSpeed());
        }
        this.speed += display_units ? Units.getSpeedUnits(event.getUnits()) : "";
        if (Units.isPace(event.getUnits())) {
            this.maxspeed = converter.convertSpeedToPace(event.getMaxSpeed());
        } else {
            this.maxspeed = String.format("%.1f", event.getMaxSpeed());
        }
        this.maxspeed += display_units ? Units.getSpeedUnits(event.getUnits()) : "";
        this.lat = String.format("%.3f", event.getLatitude());
        this.lon = String.format("%.3f", event.getLongitude());
        this.ascentrate = String.format("%.0f", event.getAscentRate() / 3600);
        this.ascentrate += display_units ? Units.getAscentRateUnits(event.getUnits()) : "";
        this.nbascent =  String.format("%d", event.getNbAscent());
        this.slope = String.format("%.1f", event.getSlope());
        this.slope += display_units ? "%" : "";
        this.accuracy = String.format("%.0f", event.getAccuracy());
        this.accuracy += display_units ? "m" : "";
        if (event.getHeartRate() < 255) {
            this.heartrate = String.format("%d", event.getHeartRate());
        } else {
            this.heartrate = "-";
        }
        if (event.getCyclingCadence() < 255) {
            this.cyclingCadence = String.format("%d", event.getCyclingCadence());
        } else {
            this.cyclingCadence = "-";
        }
        if (event.getRunningCadence() < 255) {
            this.runningCadence = String.format("%d", event.getRunningCadence());
        } else {
            this.runningCadence = "-";
        }
        if (event.getTemperature() < 255) {
            this.temperature = String.format("%.1f", event.getTemperature());
            this.temperature += display_units ? Units.getTemperatureUnits(event.getUnits()) : "";
        } else {
            this.temperature = "-";
        }
    }
}
