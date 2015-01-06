package com.njackson.utils.services;

/**
 * Created by njackson on 03/01/15.
 */
public interface IServiceStarter {
    public void startLocationServices();
    public void stopLocationServices();
    public void startRecognitionServices();
    public void stopRecognitionServices();
    public void startPebbleService();
    public void stopPebbleService();
}
