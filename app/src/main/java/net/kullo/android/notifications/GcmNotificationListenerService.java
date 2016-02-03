/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import net.kullo.android.R;
import net.kullo.android.screens.ConversationsListActivity;
import net.kullo.android.littlehelpers.KulloConstants;

public class GcmNotificationListenerService extends GcmListenerService {
    private static final String TAG = "GcmListenerService";

    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.i(TAG, "Push notification received:" + data.toString());

        Intent intent = new Intent(this, ConversationsListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(KulloConstants.ACTION_SYNC);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.kullo_notification)
                .setContentTitle(getString(R.string.notification_title_new_message))
                .setContentText(getString(R.string.notification_body_new_message))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, notificationBuilder.build());
    }
}
