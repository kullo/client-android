/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.WorkerThread;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import net.kullo.android.R;
import net.kullo.android.application.KulloApplication;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.screens.ConversationsListActivity;

import java.util.Map;

// All private methods on this call are called from onMessageReceived
// onMessageReceived is running in the background, see
// "Methods are invoked asynchronously." (https://developers.google.com/android/reference/com/google/android/gms/gcm/GcmListenerService.html)
public class MessagingService extends FirebaseMessagingService {
    private static final String TAG = MessagingService.class.getSimpleName();

    @WorkerThread
    @Override
    public void onMessageReceived(RemoteMessage message) {
        Map<String, String> data = message.getData();

        Log.i(TAG, "Push notification received: " + data.toString());

        try {
            int count = Integer.parseInt(data.get("badge"));
            KulloApplication.sharedInstance.handleBadge(count);
        } catch (NumberFormatException e) {
            Log.d(TAG, "No integer found in badge. Ignoring.");
        }

        String action = data.get("action");

        if (action != null && action.equals("new_message")) {
            if (((KulloApplication) getApplication()).foregroundActivitiesCount() == 0) {
                makeAndShowNotification();
            }
            syncIfSessionAvailable();
        } else {
            // no notification payload, sync silently
            syncIfSessionAvailable();
        }
    }

    private void makeAndShowNotification() {
        Intent openInboxAndSyncIntent = new Intent(this, ConversationsListActivity.class);
        openInboxAndSyncIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        openInboxAndSyncIntent.setAction(KulloConstants.ACTION_SYNC);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0 /* Request code */,
                openInboxAndSyncIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSmallIcon(R.drawable.kullo_notification)
                .setContentTitle(getString(R.string.notification_title_new_message))
                .setContentText(getString(R.string.notification_body_new_message))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notificationBuilder.build());
    }

    @WorkerThread
    private void syncIfSessionAvailable() {
        SessionConnector sessionConnector = SessionConnector.get();
        if (sessionConnector.sessionAvailable()) sessionConnector.sync();
    }
}
