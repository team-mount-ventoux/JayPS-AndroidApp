package com.njackson.application.modules;

/**
 * Created by server on 30/03/2014.
 */
import com.njackson.activities.MainActivity;
import com.njackson.analytics.IAnalytics;
import com.njackson.analytics.Parse;
import com.njackson.application.MainThreadBus;
import com.njackson.application.SettingsActivity;
import com.njackson.fragments.AltitudeFragment;
import com.njackson.fragments.SpeedFragment;
import com.njackson.fragments.StartButtonFragment;
import com.njackson.gps.GPSService;
import com.njackson.utils.IInstallWatchFace;
import com.njackson.utils.pebble.InstallWatchFace;
import com.njackson.virtualpebble.PebbleService;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
        injects = {MainActivity.class, SettingsActivity.class, SpeedFragment.class, AltitudeFragment.class, StartButtonFragment.class, GPSService.class, PebbleService.class},
        library = true, complete = false
)
public class PebbleBikeModule {
    // TODO put your application-specific providers here!

    @Provides @Singleton Bus providesBus() { return new MainThreadBus(new Bus(ThreadEnforcer.ANY)); }

    @Provides @Singleton IAnalytics providesAnalytics() {
        return new Parse();
    }

    @Provides @Singleton IInstallWatchFace providesWatchFaceInstall() { return new InstallWatchFace(); }

}
