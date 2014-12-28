package com.njackson.utils.pebble;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.getpebble.android.kit.PebbleKit;
import com.njackson.utils.IInstallWatchFace;
import com.njackson.utils.IMessageMaker;
import com.njackson.utils.Version;

/**
 * Created by server on 28/06/2014.
 */
public class InstallWatchFace implements IInstallWatchFace{

    private static final String TAG = "PB-InstallWatchFace";

    public void execute(Context context, IMessageMaker messageMaker) {
        try {
            context.startActivity(createIntent(context));
        } catch (ActivityNotFoundException ae) {
            messageMaker.showMessage(context, "Unable to install watchface, do you have the latest pebble app installed?");
        }
    }

    public Uri getDownloadUrl(String versionCode, String pebbleFirmwareVersion) {
        String uriString = "http://dl.pebblebike.com/p/pebblebike-1.5.0";
        uriString += ".pbw?and&v=" + versionCode;
        uriString += "&p=" + pebbleFirmwareVersion;
        Log.d(TAG, "uriString:" + uriString);

        return Uri.parse(uriString);
    }

    public Intent createIntent(Context context) {
        Uri uri = getDownloadUrl(Version.getVersionCode(context), Version.getPebbleFirmwareVersion(context));

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
