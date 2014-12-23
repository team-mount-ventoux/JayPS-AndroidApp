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

/**
 * Created by server on 28/06/2014.
 */
public class InstallWatchFace implements IInstallWatchFace{

    public void execute(Context context) {
        int versionCode;

        // Get current version code and version name
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            versionCode = packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            versionCode = 0;
        }

        try {
            String uriString;
            uriString = "http://dl.pebblebike.com/p/pebblebike-1.5.0";

            uriString += ".pbw?and&v=" + versionCode;

            Uri uri = Uri.parse(uriString);
            Intent startupIntent = new Intent();
            startupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startupIntent.setAction(Intent.ACTION_VIEW);
            startupIntent.setType("application/octet-stream");
            startupIntent.setData(uri);
            ComponentName distantActivity = new ComponentName("com.getpebble.android", "com.getpebble.android.ui.UpdateActivity");
            startupIntent.setComponent(distantActivity);
            context.startActivity(startupIntent);
        } catch (ActivityNotFoundException ae) {
            Toast.makeText(context, "Unable to install watchface, do you have the latest pebble app installed?", Toast.LENGTH_LONG).show();
        }
    }

}
