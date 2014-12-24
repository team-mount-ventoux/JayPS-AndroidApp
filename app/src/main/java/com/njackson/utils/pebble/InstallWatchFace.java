package com.njackson.utils.pebble;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.njackson.utils.IInstallWatchFace;
import com.njackson.utils.IMessageMaker;

/**
 * Created by server on 28/06/2014.
 */
public class InstallWatchFace implements IInstallWatchFace{

    public void execute(Context context, IMessageMaker messageMaker) {
        try {
            context.startActivity(createIntent());
        } catch (ActivityNotFoundException ae) {
            messageMaker.showMessage(context, "Unable to install watchface, do you have the latest pebble app installed?");
        }
    }

    public Uri getDownloadUrl() {
        String uriString;
        uriString = "http://dl.pebblebike.com/p/pebblebike-1.5.0.pbw?and&v=2";

        return Uri.parse(uriString);
    }

    public Intent createIntent() {
        Uri uri = getDownloadUrl();

        Intent startupIntent = new Intent();
        startupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startupIntent.setAction(Intent.ACTION_VIEW);
        startupIntent.setType("application/octet-stream");
        startupIntent.setData(uri);

        ComponentName distantActivity = new ComponentName("com.getpebble.android", "com.getpebble.android.ui.UpdateActivity");
        startupIntent.setComponent(distantActivity);

        return startupIntent;
    }

}
