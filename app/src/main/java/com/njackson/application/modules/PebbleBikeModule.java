package com.njackson.application.modules;

/**
 * Created by server on 30/03/2014.
 */
import com.njackson.activities.MainActivity;
import com.njackson.application.MainThreadBus;
import com.njackson.fragments.AltitudeFragment;
import com.njackson.fragments.SpeedFragment;
import com.njackson.fragments.StartButtonFragment;
import com.njackson.gps.GPSService;
import com.njackson.virtualpebble.PebbleService;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
        injects = {MainActivity.class, SpeedFragment.class, AltitudeFragment.class, StartButtonFragment.class, GPSService.class, PebbleService.class},
        library = true, complete = false
)
public class PebbleBikeModule {
    // TODO put your application-specific providers here!

    @Provides @Singleton Bus providesBus() {
        return new MainThreadBus(new Bus(ThreadEnforcer.ANY));
    }

}
