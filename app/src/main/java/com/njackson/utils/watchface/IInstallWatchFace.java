package com.njackson.utils.watchface;

import android.content.Context;

import com.njackson.utils.messages.IMessageMaker;

/**
 * Created by njackson on 23/12/14.
 */
public interface IInstallWatchFace {
    public void execute(Context context, IMessageMaker toast, String uriString);
}
