package com.njackson.gps;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.content.pm.ServiceInfo;
import android.os.Build;

import com.njackson.R;
import com.njackson.activities.MainActivity;

/**
 * Created by njackson on 06/01/15.
 */
public class MainServiceForegroundStarter implements IForegroundServiceStarter {

    private String TAG = "PB-MainServiceForegroundStarter";

    private NotificationCompat.Builder builder = null;
    private final int myID = 1000;
    private final String myChannel = "JayPS-Channel";
    private NotificationManager mNotificationManager;

    @Override
    public void startServiceForeground(Service service, String title, String contentText, int priority) {

        Intent i = new Intent(service, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mNotificationManager = (NotificationManager) service.getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel(myChannel, "jayps", NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(channel);
        }
        PendingIntent pendIntent = PendingIntent.getActivity(service, 0, i, 0);

        builder = new NotificationCompat.Builder(service, myChannel);

        builder.setContentTitle(title).setContentText(contentText)
                .setSmallIcon(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP ? R.drawable.ic_notification : R.drawable.ic_launcher)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(false)
                .setOngoing(true)
                .setPriority(priority)
                .setContentIntent(pendIntent);
        Notification notification = builder.build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            service.startForeground(myID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION | ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC | ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
        } else {
            service.startForeground(myID, notification);
        }
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
