package com.njackson;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import com.google.android.gms.location.*;

/**
 * Created with IntelliJ IDEA.
 * User: server
 * Date: 19/05/2013
 * Time: 21:38
 * To change this template use File | Settings | File Templates.
 */
public class ActivityRecognitionIntentService extends IntentService {

    private static boolean _watchShown;

    public ActivityRecognitionIntentService() {
        super("ActivityRecognitionIntentService");
        Log.d("MainActivity","Start");
    }

    public ActivityRecognitionIntentService(String name) {
        super(name);
        Log.d("MainActivity","Start");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            //sendReply(DetectedActivity.ON_BICYCLE);

            Log.d("MainActivity","Handle Intent");

            switch(result.getMostProbableActivity().getType()) {

                case DetectedActivity.ON_BICYCLE:
                    //TODO: start pebble watch face
                    // start the watch face
                    Log.d("ActivityIntent","ON_BICYCLE");
                    sendReply(result.getMostProbableActivity().getType());
                    break;
                case DetectedActivity.TILTING:
                    Log.d("ActivityIntent","TILTING");
                    break;
                case DetectedActivity.STILL:
                    Log.d("ActivityIntent","STILL");
                default:
                    sendReply(result.getMostProbableActivity().getType());
            }

        }
    }

    private void sendReply(int type) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.ActivityRecognitionReceiver.ACTION_RESP);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra("ACTIVITY_CHANGED", type);
        sendBroadcast(broadcastIntent);
    }

}
