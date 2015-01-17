package com.njackson.gps;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.google.common.base.Function;

import java.util.concurrent.Callable;

import fr.jayps.android.AdvancedLocation;

/**
 * Created by njackson on 24/12/14.
 */
public class GPSSensorEventListener implements SensorEventListener{

    private static final String TAG = "PB-GPSSensorEventListener";

    private AdvancedLocation _advancedLocation;
    private Callable  _callback;
    private SensorManager _sensorManager;

    public GPSSensorEventListener(AdvancedLocation advancedLocation, SensorManager sensorManager, Callable callback){
        _advancedLocation = advancedLocation;
        _callback = callback;
        _sensorManager = sensorManager;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /* It is not possible to mock the sensor event so we need to remove this as a dependency from our tests */
    @Override
    public void onSensorChanged(SensorEvent event) {
        sensorChanged(event.sensor.getType(), event.values);
    }

    public void sensorChanged(int sensorType, float[] values) {
        if(sensorType == Sensor.TYPE_PRESSURE) {
            float pressure_value = values[0];
            double altitude = _sensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure_value);
            _advancedLocation.onAltitudeChanged(altitude);

            try {
                _callback.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
