package com.njackson.application.modules;

/**
 * Created by server on 30/03/2014.
 */
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.RecordingApi;
import com.njackson.activities.MainActivity;
import com.njackson.activityrecognition.ActivityRecognitionIntentService;
import com.njackson.activityrecognition.ActivityRecognitionService;
import com.njackson.analytics.IAnalytics;
import com.njackson.analytics.Parse;
import com.njackson.application.MainThreadBus;
import com.njackson.application.SettingsActivity;
import com.njackson.fit.GoogleFitService;
import com.njackson.fragments.AltitudeFragment;
import com.njackson.fragments.SpeedFragment;
import com.njackson.fragments.StartButtonFragment;
import com.njackson.gps.GPSService;
import com.njackson.live.LiveService;
import com.njackson.utils.googleplay.GooglePlayServices;
import com.njackson.utils.googleplay.IGooglePlayServices;
import com.njackson.utils.timer.ITimer;
import com.njackson.utils.timer.Timer;
import com.njackson.utils.watchface.IInstallWatchFace;
import com.njackson.utils.watchface.InstallPebbleWatchFace;
import com.njackson.utils.version.AndroidVersion;
import com.njackson.utils.version.PebbleVersion;
import com.njackson.virtualpebble.PebbleService;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
        injects = {MainActivity.class, SettingsActivity.class, SpeedFragment.class, AltitudeFragment.class, StartButtonFragment.class, GPSService.class, PebbleService.class, LiveService.class, GoogleFitService.class, ActivityRecognitionService.class, ActivityRecognitionIntentService.class},
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

    @Provides ITimer providesTimer() { return new Timer(); }
}
