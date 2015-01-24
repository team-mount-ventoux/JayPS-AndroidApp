package com.njackson.gps;

import android.app.Service;

/**
 * Created by njackson on 06/01/15.
 */
public interface IForegroundServiceStarter {
    public void startServiceForeground(Service context, String title, String contentText);
    public void stopServiceForeground(Service context);
}
