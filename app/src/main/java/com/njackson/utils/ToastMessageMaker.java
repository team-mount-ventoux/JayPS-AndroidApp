package com.njackson.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by njackson on 24/12/14.
 */
public class ToastMessageMaker implements IMessageMaker {
    @Override
    public void showMessage(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
