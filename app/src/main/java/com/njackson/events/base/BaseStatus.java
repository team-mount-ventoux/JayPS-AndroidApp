package com.njackson.events.base;

/**
 * Created by njackson on 24/01/15.
 */
public class BaseStatus {
    public enum Status {
        STARTED,
        STOPPED,
        DISABLED
    }

    public Status _status;
    public Status getStatus() { return _status; }

    public BaseStatus(Status status) {
        this._status = status;
    }
}
