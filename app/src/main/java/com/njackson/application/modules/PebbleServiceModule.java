package com.njackson.application.modules;

import com.njackson.virtualpebble.IMessageManager;
import com.njackson.virtualpebble.MessageManager;
import com.njackson.virtualpebble.PebbleService;

import dagger.Module;
import dagger.Provides;

/**
 * Created by server on 28/06/2014.
 */
@Module(
        injects = {PebbleService.class},
        library = true, complete = false
)
public class PebbleServiceModule {

    @Provides
    public IMessageManager providesMessageManager() {
        return new MessageManager();
    }

}
