package com.njackson.changelog;

import android.app.Activity;

/**
 * Created by njackson on 31/01/15.
 */
public interface IChangeLogBuilder {
    IChangeLogBuilder setActivity(Activity activity);
    IChangeLog build();
}
