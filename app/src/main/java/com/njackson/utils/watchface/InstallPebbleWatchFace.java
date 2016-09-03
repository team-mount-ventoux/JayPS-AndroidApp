package com.njackson.utils.watchface;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.njackson.utils.messages.IMessageMaker;
import com.njackson.utils.version.IAndroidVersion;
import com.njackson.utils.version.IWatchFaceVersion;

/**
 * Created by server on 28/06/2014.
 */
public class InstallPebbleWatchFace implements IInstallWatchFace{

    private final String TAG = "PB-InstallWatchFace";
    private IAndroidVersion _androidVersion;
    private IWatchFaceVersion _watchFaceVersion;

    public InstallPebbleWatchFace(IAndroidVersion androidVersion, IWatchFaceVersion watchFaceVersion) {
        _androidVersion = androidVersion;
        _watchFaceVersion = watchFaceVersion;
    }

    public void execute(Context context, IMessageMaker messageMaker, String uriString) {
        try {
            context.startActivity(createIntent(context, uriString));
        } catch (ActivityNotFoundException ae) {
            messageMaker.showMessage(context, "Unable to install watchface, do you have the latest pebble app installed?");
        }
    }

    public Uri getDownloadUrl(String versionCode, String pebbleFirmwareVersion, String uriString) {
        uriString += "?and&v=" + versionCode;
        uriString += "&p=" + pebbleFirmwareVersion;
        Log.d(TAG, "uriString:" + uriString);

        return Uri.parse(uriString);
    }

    public Intent createIntent(Context context, String uriString) {
        Uri uri = getDownloadUrl(_androidVersion.getVersionCode(context), _watchFaceVersion.getFirmwareVersion(context), uriString);

        Intent startupIntent = new Intent();
        startupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startupIntent.setAction(Intent.ACTION_VIEW);
        startupIntent.setDataAndType(uri, "application/octet-stream");

        // For Pebble ComponentName distantActivity = new ComponentName("com.getpebble.android", "com.getpebble.android.ui.UpdateActivity");
        // For Pebble Time ComponentName distantActivity = new ComponentName("com.getpebble.android.basalt", "com.getpebble.android.ui.UpdateActivity");
        //startupIntent.setComponent(distantActivity);

        return startupIntent;
    }

}
