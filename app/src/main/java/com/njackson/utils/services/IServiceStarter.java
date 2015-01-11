package com.njackson.utils.services;

/**
 * Created by njackson on 03/01/15.
 */
public interface IServiceStarter {
    public void startEssentialServices();
    public void stopEssentialServices();
    public void startLocationServices();
    public void stopLocationServices();
}
