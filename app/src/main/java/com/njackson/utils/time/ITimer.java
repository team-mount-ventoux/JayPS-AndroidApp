package com.njackson.utils.time;

/**
 * Created by njackson on 03/01/15.
 */
public interface ITimer {
    public boolean getActive();
    public void setTimer(long timeoutMilliseconds, ITimerHandler handler);
    public void setRepeatingTimer(long timeoutMilliseconds, ITimerHandler handler);
    public boolean cancel();
}
