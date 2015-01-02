package com.njackson.application.modules;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.BaseImplementation;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.api.d;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionApi;
import com.njackson.application.SettingsActivity;
import com.njackson.gps.GPSService;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static android.content.Context.LOCATION_SERVICE;
import static android.content.Context.SENSOR_SERVICE;

/**
 * Created by server on 30/03/2014.
 */
@Module(library = true,complete=false,injects = {GPSService.class, SettingsActivity.class})
public class AndroidModule {
    private final PebbleBikeApplication application;

    public AndroidModule(PebbleBikeApplication application) {
        this.application = application;
    }

    /**
     * Allow the application context to be injected but require that it be annotated with
     * {@link ForApplication @Annotation} to explicitly differentiate it from an activity context.
     */
    @Provides @Singleton @ForApplication
    Context provideApplicationContext() {
        return application;
    }

    @Provides @Singleton LocationManager provideLocationManager() {
        return (LocationManager) application.getSystemService(LOCATION_SERVICE);
    }

    @Provides @Singleton SensorManager provideSensorManager() {
        return (SensorManager) application.getSystemService(SENSOR_SERVICE);
    }

    @Provides @Singleton SharedPreferences provideSharedPreferences() {
        return application.getSharedPreferences("com.njackson_preferences", Context.MODE_PRIVATE);
    }

    @Provides @Singleton
    GoogleApiClient provideActivityRecognitionClient() {
        return new GoogleApiClient.Builder(application).addApi(ActivityRecognition.API).build();
    }
}