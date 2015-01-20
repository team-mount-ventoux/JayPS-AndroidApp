package com.njackson.utils.services;

/**
 * Created by njackson on 03/01/15.
 */
public interface IServiceStarter {
    public void startPebbleServices();
    public void stopPebbleServices();
    public void startActivityServices();
    public void stopActivityServices();
    public void startLocationServices();
    public void stopLocationServices();
    public boolean serviceRunning(Class<?> serviceClass);
}
