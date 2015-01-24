package com.njackson.application.modules;

/**
 * Created by server on 30/03/2014.
 */
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.RecordingApi;
import com.google.android.gms.fitness.SessionsApi;
import com.njackson.activities.MainActivity;
import com.njackson.activityrecognition.ActivityRecognitionIntentService;
import com.njackson.activityrecognition.ActivityRecognitionService;
import com.njackson.analytics.IAnalytics;
import com.njackson.analytics.Parse;
import com.njackson.application.MainThreadBus;
import com.njackson.activities.SettingsActivity;
import com.njackson.fit.GoogleFitService;
import com.njackson.fragments.AltitudeFragment;
import com.njackson.fragments.SpeedFragment;
import com.njackson.fragments.StartButtonFragment;
import com.njackson.gps.GPSServiceCommand;
import com.njackson.gps.MainServiceForegroundStarter;
import com.njackson.gps.IForegroundServiceStarter;
import com.njackson.live.LiveService;
import com.njackson.oruxmaps.OruxMapsService;
import com.njackson.pebble.PebbleDataReceiver;
import com.njackson.pebble.PebbleServiceCommand;
import com.njackson.pebble.canvas.CanvasWrapper;
import com.njackson.pebble.canvas.ICanvasWrapper;
import com.njackson.service.MainService;
import com.njackson.utils.googleplay.GooglePlayServices;
import com.njackson.utils.googleplay.IGooglePlayServices;
import com.njackson.utils.time.ITime;
import com.njackson.utils.time.ITimer;
import com.njackson.utils.time.Time;
import com.njackson.utils.time.Timer;
import com.njackson.utils.watchface.IInstallWatchFace;
import com.njackson.utils.watchface.InstallPebbleWatchFace;
import com.njackson.utils.version.AndroidVersion;
import com.njackson.utils.version.PebbleVersion;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
        injects = {MainActivity.class, SettingsActivity.class, SpeedFragment.class, AltitudeFragment.class, StartButtonFragment.class, GPSServiceCommand.class, PebbleServiceCommand.class, LiveService.class, GoogleFitService.class, ActivityRecognitionService.class, ActivityRecognitionIntentService.class, OruxMapsService.class, PebbleDataReceiver.class, MainService.class},
        library = true, complete = false
)
public class PebbleBikeModule {
    @Provides @Singleton Bus providesBus() { return new MainThreadBus(new Bus(ThreadEnforcer.ANY)); }

    @Provides @Singleton IAnalytics providesAnalytics() {
        return new Parse();
    }

    @Provides IInstallWatchFace providesWatchFaceInstall() { return new InstallPebbleWatchFace(new AndroidVersion(), new PebbleVersion()); }

    @Provides IGooglePlayServices providesGooglePlayServices() { return new GooglePlayServices(); }

    @Provides RecordingApi providesGoogleFitRecordingApi() { return Fitness.RecordingApi; }

    @Provides SessionsApi providesGoogleFitSessionsApi() { return Fitness.SessionsApi; }

    @Provides ITimer providesTimer() { return new Timer(); }

    @Provides ITime providesTime() { return new Time(); }

    @Provides IForegroundServiceStarter providesForegroundServiceStarter() { return new MainServiceForegroundStarter(); }

    @Provides ICanvasWrapper providesCanvasWrapper() { return new CanvasWrapper(); }
}
