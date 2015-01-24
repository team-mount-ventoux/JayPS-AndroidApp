package com.njackson.service;

import com.njackson.application.IInjectionContainer;

/**
 * Created by njackson on 24/01/15.
 */
public interface IServiceCommand {
    public void execute(IInjectionContainer container);
}
