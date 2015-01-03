package com.njackson.utils.timer;

import java.util.TimerTask;

/**
 * Created by njackson on 03/01/15.
 */
public class Timer implements ITimer{


    private java.util.Timer _timer;
    private boolean _active;

    @Override
    public boolean getActive() {
        return _active;
    }

    @Override
    public void setTimer(long timeoutMilliseconds, final ITimerHandler handler) {
        _timer = new java.util.Timer();
        _timer.schedule(new TimerTask() {
            @Override
            public void run() {
                _active = false;
                handler.handleTimeout();
            }
        },timeoutMilliseconds);

        _active = true;
    }

    @Override
    public boolean cancel() {
        if(_timer != null) {
            _timer.cancel();
            _active = false;
            return true;
        } else {
            return false;
        }
    }
}
