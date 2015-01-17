package com.njackson.application.modules;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.location.LocationManager;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.location.ActivityRecognition;
import com.njackson.activities.MainActivity;
import com.njackson.activityrecognition.ActivityRecognitionService;
import com.njackson.application.PebbleBikeApplication;
import com.njackson.application.SettingsActivity;
import com.njackson.gps.GPSService;
import com.njackson.oruxmaps.IOruxMaps;
import com.njackson.oruxmaps.OruxMaps;
import com.njackson.oruxmaps.OruxMapsService;
import com.njackson.utils.googleplay.GoogleFitSessionManager;
import com.njackson.utils.googleplay.GooglePlayServices;
import com.njackson.utils.googleplay.IGoogleFitSessionManager;
import com.njackson.utils.services.IServiceStarter;
import com.njackson.utils.services.ServiceStarter;
import com.njackson.pebble.IMessageManager;
import com.njackson.pebble.MessageManager;
import com.njackson.pebble.PebbleService;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static android.content.Context.LOCATION_SERVICE;
import static android.content.Context.SENSOR_SERVICE;

/**
 * Created by server on 30/03/2014.
 */
@Module(library = true,complete=false,injects = {GPSService.class, ActivityRecognitionService.class, MainActivity.class, SettingsActivity.class, PebbleService.class, OruxMapsService.class})
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

    @Provides @Singleton @Named("GoogleActivity")
    GoogleApiClient provideActivityRecognitionClient() {
        return new GoogleApiClient.Builder(application).addApi(ActivityRecognition.API).build();
    }

    @Provides @Singleton @Named("GoogleFit")
    GoogleApiClient provideFitnessAPIClient() {
        return new GoogleApiClient.Builder(application)
                .addApi(Fitness.API)
                .addScope(Fitness.SCOPE_ACTIVITY_READ)
                .addScope(Fitness.SCOPE_BODY_READ_WRITE)
                .build();
    }

    @Provides
    IGoogleFitSessionManager providesGoogleFitSessionManager() { return new GoogleFitSessionManager(application, new GooglePlayServices(), Fitness.SessionsApi); }

    @Provides @Singleton
    IServiceStarter provideServiceStarter() { return new ServiceStarter(application, provideSharedPreferences()); }

    @Provides
    public IMessageManager providesMessageManager() { return new MessageManager(application); }

    @Provides IOruxMaps providesOruxMaps() { return new OruxMaps(application); }
}