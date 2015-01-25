package com.njackson.service;

import com.njackson.application.IInjectionContainer;
import com.njackson.events.base.BaseStatus;

/**
 * Created by njackson on 24/01/15.
 */
public interface IServiceCommand {
    public void execute(IInjectionContainer container);
    public void dispose();
    public BaseStatus.Status getStatus();
}
