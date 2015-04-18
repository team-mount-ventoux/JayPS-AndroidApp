package com.njackson.utils.services;

/**
 * Created by njackson on 03/01/15.
 */
public interface IServiceStarter {
    public void startActivityService();
    public void stopActivityService();
    public boolean isLocationServicesRunning();
    public void startLocationServices();
    public void stopLocationServices();
    public void broadcastLocationState();
    public boolean serviceRunning(Class<?> serviceClass);
}
