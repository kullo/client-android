/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.notifications;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import net.kullo.android.R;
import net.kullo.android.kulloapi.SessionConnector;

import java.io.IOException;

public class GcmRegistrationIntentService extends IntentService {
    private static final String TAG = "GcmRegService";

    public GcmRegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!SessionConnector.get().sessionAvailable()) {
            // Google Play Services might relaunch this thread if token changed
            // while the session was inactive.  Ignore this case.
            return;
        }

        // Do not call InstanceID.getToken() on the main thread.
        InstanceID instanceID = InstanceID.getInstance(this);
        final String token;

        try {
            // gcm_defaultSenderId is derived from google-services.json
            token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
        } catch (IOException e) {
            Log.e(TAG, "Failed to get a new token", e);
            return;
        }

        Log.i(TAG, "GCM registration token: " + token);
        new Handler(getApplicationContext().getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                SessionConnector.get().registerPushToken(token);
            }
        });
    }
}
