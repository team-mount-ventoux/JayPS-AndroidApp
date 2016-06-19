package com.njackson.gps;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.njackson.R;
import com.njackson.activities.MainActivity;

/**
 * Created by njackson on 06/01/15.
 */
public class MainServiceForegroundStarter implements IForegroundServiceStarter {

    private String TAG = "PB-MainServiceForegroundStarter";

    private NotificationCompat.Builder builder = null;
    private final int myID = 1000;

    @Override
    public void startServiceForeground(Service service, String title, String contentText, int priority) {

        Intent i = new Intent(service, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendIntent = PendingIntent.getActivity(service, 0, i, 0);

        builder = new NotificationCompat.Builder(service);

        builder.setContentTitle(title).setContentText(contentText)
                .setSmallIcon(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP ? R.drawable.ic_notification : R.drawable.ic_launcher)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(false)
                .setOngoing(true)
                .setPriority(priority)
                .setContentIntent(pendIntent);
        Notification notification = builder.build();

       service.startForeground(myID, notification);
    }

    @Override
    public void stopServiceForeground(Service service) {
        service.stopForeground(true);
        builder = null;
    }

    public void changeNotification(Service context, String text, int priority) {
        if (builder != null) {
            builder.setContentText(text);
            builder.setPriority(priority);

            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(myID, builder.build());
        }
    }
}
