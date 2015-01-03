package com.njackson.utils.timer;

/**
 * Created by njackson on 03/01/15.
 */
public interface ITimer {
    public boolean getActive();
    public void setTimer(long timeoutMilliseconds, ITimerHandler handler);
    public boolean cancel();
}
