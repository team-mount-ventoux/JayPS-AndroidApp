package com.njackson.gps;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.njackson.R;
import com.njackson.activities.MainActivity;

/**
 * Created by njackson on 06/01/15.
 */
public class MainServiceForegroundStarter implements IForegroundServiceStarter {

    @Override
    public void startServiceForeground(Service service, String title, String contentText) {
        final int myID = 1000;

        Intent i = new Intent(service, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendIntent = PendingIntent.getActivity(service, 0, i, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(service);

        builder.setContentTitle(title).setContentText(contentText)
                .setSmallIcon(R.drawable.ic_launcher)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(pendIntent);
        Notification notification = builder.build();

       service.startForeground(myID, notification);
    }

    @Override
    public void stopServiceForeground(Service service) {
        service.stopForeground(true);
    }

}
