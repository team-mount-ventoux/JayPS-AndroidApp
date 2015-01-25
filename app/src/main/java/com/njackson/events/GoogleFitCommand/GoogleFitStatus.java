package com.njackson.events.GoogleFitCommand;

import com.google.android.gms.common.ConnectionResult;
import com.njackson.events.base.BaseStatus;

/**
 * Created by server on 21/03/2014.
 */
public class GoogleFitStatus extends BaseStatus{

    private ConnectionResult _connectionResult = null;

    public GoogleFitStatus(Status status) {
        super(status);
    }

    public GoogleFitStatus(Status status, ConnectionResult connectionResult) {
        super(status);
        _connectionResult = connectionResult;
    }

    public ConnectionResult getConnectionResult() { return _connectionResult; }
}
