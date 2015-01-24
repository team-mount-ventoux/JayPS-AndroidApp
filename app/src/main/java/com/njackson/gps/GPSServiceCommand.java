package com.njackson.gps;

import com.njackson.application.IInjectionContainer;
import com.njackson.service.IServiceCommand;

/**
 * Created by njackson on 24/01/15.
 */
public class GPSServiceCommand implements IServiceCommand {



    @Override
    public void execute(IInjectionContainer container) {
        container.inject(this);
    }
}
