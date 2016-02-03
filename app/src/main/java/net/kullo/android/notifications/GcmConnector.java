/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.notifications;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import android.app.Activity;

import net.kullo.android.kulloapi.SessionConnector;

public class GcmConnector {
    private static final String TAG = "GcmConnector";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private static final GcmConnector SINGLETON = new GcmConnector();
    @NonNull
    public static GcmConnector get() {
        return SINGLETON;
    }

    private static boolean mHasGooglePlay = false;

    // check gplay at app start, prompt user if not there
    public void checkGooglePlayAndPrompt(Activity activity) {
        GoogleApiAvailability playAPI = GoogleApiAvailability.getInstance();
        int result = playAPI.isGooglePlayServicesAvailable(activity);
        if (result != ConnectionResult.SUCCESS) {
            if (playAPI.isUserResolvableError(result)) {
                playAPI.getErrorDialog(activity, result, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "Failed to connect to Google Play Services");
            }
        } else {
            mHasGooglePlay = true;
        }
    }

    // if conditions are right, launch service that will retrieve a new token
    public void fetchToken(Context context) {
        if (!mHasGooglePlay) {
            Log.i(TAG, "Device does not have Google Play Services. Skipping Push notifications.");
            return;
        }

        // make sure that session is created
        if (!SessionConnector.get().sessionAvailable()) {
            Log.e(TAG, "Trying to retrieve new token without a valid session");
            return;
        }

        // check if session already has a valid token, if it does return
        // if the token changes, GcmInstanceIdListenerService will notify the token service
        if (SessionConnector.get().hasPushToken()) {
            return;
        }

        // launch token service
        Intent intent = new Intent(context, GcmRegistrationIntentService.class);
        context.startService(intent);
    }
}
