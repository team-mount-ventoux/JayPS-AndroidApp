package com.njackson.application.modules;

import com.njackson.gps.GPSServerStarterForeground;
import com.njackson.gps.IGPSServiceStarterForeground;
import android.content.Context;
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

    private Context _context;

    public PebbleServiceModule(Context context) {
        _context = context;
    }

    @Provides
    public IMessageManager providesMessageManager() {
        return new MessageManager(_context);
    }

    @Provides
    IGPSServiceStarterForeground providesForegroundServiceStarter() { return new GPSServerStarterForeground(); }

}
