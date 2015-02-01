package com.njackson.changelog;

import android.app.AlertDialog;

import de.cketti.library.changelog.ChangeLog;

/**
 * Created by njackson on 31/01/15.
 */
public class CLChangeLog implements IChangeLog {

    private final ChangeLog _changeLog;

    public CLChangeLog(ChangeLog changeLog) {
        _changeLog = changeLog;
    }

    @Override
    public boolean isFirstRun() {
        return _changeLog.isFirstRun();
    }

    @Override
    public AlertDialog getDialog() {
        return _changeLog.getLogDialog();
    }
}
