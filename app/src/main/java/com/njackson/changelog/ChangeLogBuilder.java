package com.njackson.changelog;

import android.app.Activity;

import de.cketti.library.changelog.ChangeLog;

/**
 * Created by njackson on 31/01/15.
 */
public class ChangeLogBuilder implements IChangeLogBuilder {

    private Activity _activity;

    @Override
    public IChangeLogBuilder setActivity(Activity activity) {
        _activity = activity;
        return this;
    }

    @Override
    public IChangeLog build() {
        return new CLChangeLog(new ChangeLog(_activity));
    }
}
