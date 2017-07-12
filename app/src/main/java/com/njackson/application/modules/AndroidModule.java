package com.njackson.application.modules;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.RecordingApi;
import com.google.android.gms.fitness.SessionsApi;
import com.google.android.gms.location.ActivityRecognition;
import com.njackson.activities.MainActivity;
import com.njackson.activityrecognition.ActivityRecognitionIntentService;
import com.njackson.activityrecognition.ActivityRecognitionServiceCommand;
import com.njackson.analytics.IAnalytics;
import com.njackson.analytics.MyParse;
import com.njackson.application.MainThreadBus;
import com.njackson.application.PebbleBikeApplication;
import com.njackson.activities.SettingsActivity;
import com.njackson.changelog.ChangeLogBuilder;
import com.njackson.changelog.IChangeLogBuilder;
import com.njackson.fit.GoogleFitServiceCommand;
import com.njackson.fragments.AltitudeFragment;
import com.njackson.fragments.SpeedFragment;
import com.njackson.fragments.StartButtonFragment;
import com.njackson.gps.GPSServiceCommand;
import com.njackson.gps.IForegroundServiceStarter;
import com.njackson.gps.MainServiceForegroundStarter;
import com.njackson.gps.Navigator;
import com.njackson.sensor.Ble;
import com.njackson.sensor.BLEServiceCommand;
import com.njackson.sensor.IBle;
import com.njackson.live.ILiveTracking;
import com.njackson.live.LiveServiceCommand;
import com.njackson.live.LiveTracking;
import com.njackson.oruxmaps.IOruxMaps;
import com.njackson.oruxmaps.OruxMaps;
import com.njackson.oruxmaps.OruxMapsServiceCommand;
import com.njackson.pebble.PebbleDataReceiver;
import com.njackson.pebble.PebbleServiceCommand;
import com.njackson.pebble.canvas.CanvasWrapper;
import com.njackson.pebble.canvas.ICanvasWrapper;
import com.njackson.service.IServiceCommand;
import com.njackson.service.MainService;
import com.njackson.state.GPSDataStore;
import com.njackson.state.IGPSDataStore;
import com.njackson.upload.RunkeeperUpload;
import com.njackson.upload.StravaUpload;
import com.njackson.utils.AltitudeGraphReduce;
import com.njackson.utils.BootUpReceiver;
import com.njackson.utils.googleplay.GoogleFitSessionManager;
import com.njackson.utils.googleplay.GooglePlayServices;
import com.njackson.utils.googleplay.IGoogleFitSessionManager;
import com.njackson.utils.googleplay.IGooglePlayServices;
import com.njackson.utils.services.IServiceStarter;
import com.njackson.utils.services.ServiceStarter;
import com.njackson.pebble.IMessageManager;
import com.njackson.pebble.MessageManager;
import com.njackson.utils.time.ITime;
import com.njackson.utils.time.ITimer;
import com.njackson.utils.time.Time;
import com.njackson.utils.time.Timer;
import com.njackson.utils.version.AndroidVersion;
import com.njackson.utils.version.PebbleVersion;
import com.njackson.utils.watchface.IInstallWatchFace;
import com.njackson.utils.watchface.InstallPebbleWatchFace;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static android.content.Context.LOCATION_SERVICE;
import static android.content.Context.SENSOR_SERVICE;

/**
 * Created by server on 30/03/2014.
 */
@Module(library = true,complete=false,injects = {
        PebbleBikeApplication.class,
        MainActivity.class,
        SettingsActivity.class,
        StartButtonFragment.class,
        SpeedFragment.class,
        AltitudeFragment.class,
        MainService.class,
        ActivityRecognitionIntentService.class,
        GPSServiceCommand.class,
        PebbleServiceCommand.class,
        LiveServiceCommand.class,
        OruxMapsServiceCommand.class,
        GoogleFitServiceCommand.class,
        ActivityRecognitionServiceCommand.class,
        PebbleDataReceiver.class,
        BLEServiceCommand.class,
        Ble.class,
        SpeedFragment.class,
        MessageManager.class,
        BootUpReceiver.class,
        StravaUpload.class,
        RunkeeperUpload.class
        })
public class AndroidModule {
    private final String TAG = "PB-AndroidModule";

    private PebbleBikeApplication application = null;

    public AndroidModule(){}
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

    @Provides @Singleton
    Bus providesBus() { return new MainThreadBus(new Bus(ThreadEnforcer.ANY)); }

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
    IGPSDataStore providesGPSDataStore(SharedPreferences preferences) { return new GPSDataStore(preferences, application); }

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
    IServiceStarter provideServiceStarter(Bus bus, SharedPreferences preferences) {
        return new ServiceStarter(application, preferences, bus);
    }
    @Provides @Singleton
    AltitudeGraphReduce providesAltitudeGraphReduce() { return new AltitudeGraphReduce(); }

    @Provides @Singleton
    public IMessageManager providesMessageManager(SharedPreferences preferences) { return new MessageManager(preferences, application); }

    @Provides IOruxMaps providesOruxMaps() { return new OruxMaps(application); }

    @Provides @Singleton Navigator providesNavigator() { return new Navigator(); }

    @Provides
    IBle providesHrm() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // BLE requires 4.3 (Api level 18)
            try {
                return new Ble(application);
            } catch (NoClassDefFoundError e) {
                // bug with some 4.1/4.2 devices that report Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 !!!
                Log.e(TAG, "NoClassDefFoundError: " + e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    @Provides
    @Named("LiveTrackingMmt")
    ILiveTracking provideLiveTrackingMmt() {
        return new LiveTracking(application, LiveTracking.TYPE_MMT, providesBus());
    }

    @Provides
    @Named("LiveTrackingJayPS")
    ILiveTracking provideLiveTrackingJayps() {
        return new LiveTracking(application, LiveTracking.TYPE_JAYPS, providesBus());
    }

    @Provides @Singleton
    IAnalytics providesAnalytics() {
        return new MyParse();
    }

    @Provides
    IInstallWatchFace providesWatchFaceInstall() { return new InstallPebbleWatchFace(new AndroidVersion(), new PebbleVersion()); }

    @Provides
    IGooglePlayServices providesGooglePlayServices() { return new GooglePlayServices(); }

    @Provides
    RecordingApi providesGoogleFitRecordingApi() { return Fitness.RecordingApi; }

    @Provides
    SessionsApi providesGoogleFitSessionsApi() { return Fitness.SessionsApi; }

    @Provides
    ITimer providesTimer() { return new Timer(); }

    @Provides
    ITime providesTime() { return new Time(); }

    @Provides
    IForegroundServiceStarter providesForegroundServiceStarter() { return new MainServiceForegroundStarter(); }

    @Provides
    ICanvasWrapper providesCanvasWrapper() { return new CanvasWrapper(); }

    @Provides
    IChangeLogBuilder providesChangeLogBuilder() { return new ChangeLogBuilder(); }

    @Provides
    List<IServiceCommand> providesServiceCommands() {
        return Arrays.asList(
                new GPSServiceCommand(),
                new PebbleServiceCommand(),
                new ActivityRecognitionServiceCommand(),
                new OruxMapsServiceCommand(),
                new LiveServiceCommand(),
                new GoogleFitServiceCommand(),
                new BLEServiceCommand()
        );
    }
}