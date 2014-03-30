package com.pebblebike.application;

/**
 * Created by server on 30/03/2014.
 */
import com.pebblebike.activities.MainActivity;
import com.squareup.otto.Bus;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
        injects = MainActivity.class,
        library = true
)
public class PebbleBikeModule {
    // TODO put your application-specific providers here!

    @Provides @Singleton Bus providesBus() {
        return new Bus();
    }

}
