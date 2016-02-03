/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.notifications;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import net.kullo.android.R;
import net.kullo.android.kulloapi.SessionConnector;

public class GcmRegistrationIntentService extends IntentService {
    private static final String TAG = "GcmRegService";

    public GcmRegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            if (!SessionConnector.get().sessionAvailable()) {
                // Google Play Services might relaunch this thread if token changed
                // while the session was inactive.  Ignore this case.
                return;
            }

            // gcm_defaultSenderId is derived from google-services.json
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            Log.i(TAG, "GCM Registration Token: " + token);

            SessionConnector.get().registerPushToken(token);
        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
        }
    }
}
