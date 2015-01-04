package com.njackson.utils.messages;

import android.content.Context;
import android.widget.Toast;

import com.njackson.utils.messages.IMessageMaker;

/**
 * Created by njackson on 24/12/14.
 */
public class ToastMessageMaker implements IMessageMaker {
    @Override
    public void showMessage(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
