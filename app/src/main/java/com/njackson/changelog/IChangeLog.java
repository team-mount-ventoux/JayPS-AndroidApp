package com.njackson.changelog;

import android.app.AlertDialog;

/**
 * Created by njackson on 31/01/15.
 */
public interface IChangeLog {

    boolean isFirstRun();
    AlertDialog getDialog();

}
