/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.notifications;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.javautils.RuntimeAssertion;

public class GcmConnector {
    private static final String TAG = "GcmConnector";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    public enum NotAvailableReason {
        Disabled,
        Missing,
        UpdateRequired,
        Updating,
        Unknown,
    }

    /* singleton setup */
    private static final GcmConnector SINGLETON = new GcmConnector();
    @NonNull public static GcmConnector get() {
        return SINGLETON;
    }

    /* members */
    @Nullable private Boolean mGooglePlayOk = null;
    @Nullable private NotAvailableReason mGooglePlayNotAvailableReason = null;

    // check gplay at app start, prompt user if not there
    @CheckResult
    public Dialog checkGooglePlayAvailabilityAndPrompt(Activity activity) {
        GoogleApiAvailability playAPI = GoogleApiAvailability.getInstance();
        int result = playAPI.isGooglePlayServicesAvailable(activity);
        if (result != ConnectionResult.SUCCESS) {
            switch (result) {
                case ConnectionResult.SERVICE_DISABLED:
                    mGooglePlayNotAvailableReason = NotAvailableReason.Disabled;
                    break;
                case ConnectionResult.SERVICE_MISSING:
                    mGooglePlayNotAvailableReason = NotAvailableReason.Missing;
                    break;
                case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                    mGooglePlayNotAvailableReason = NotAvailableReason.UpdateRequired;
                    break;
                case ConnectionResult.SERVICE_UPDATING:
                    mGooglePlayNotAvailableReason = NotAvailableReason.Updating;
                    break;
                default:
                    mGooglePlayNotAvailableReason = NotAvailableReason.Unknown;
                    break;
            }

            if (playAPI.isUserResolvableError(result)) {
                return playAPI.getErrorDialog(activity, result, PLAY_SERVICES_RESOLUTION_REQUEST);
            } else {
                Log.i(TAG, "Failed to connect to Google Play Services");
                return null;
            }
        } else {
            mGooglePlayOk = true;
            return null;
        }
    }

    public boolean googlePlayAvailable() {
        RuntimeAssertion.require(mGooglePlayOk != null);
        return mGooglePlayOk;
    }

    public NotAvailableReason googlePlayNotAvailableReason() {
        return mGooglePlayNotAvailableReason;
    }

    // if conditions are right, launch service that will retrieve a new token
    public void fetchAndRegisterToken(Context context) {
        RuntimeAssertion.require(SessionConnector.get().sessionAvailable());
        checkGooglePlay(context);

        RuntimeAssertion.require(mGooglePlayOk != null);
        if (!mGooglePlayOk) {
            Log.i(TAG, "Device does not have Google Play Services. Skipping Push notifications.");
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

    public void removeAllNotifications(Context context) {
        ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
    }

    private void checkGooglePlay(Context context) {
        if (mGooglePlayOk == null) {
            GoogleApiAvailability playAPI = GoogleApiAvailability.getInstance();
            int result = playAPI.isGooglePlayServicesAvailable(context);
            mGooglePlayOk = (result == ConnectionResult.SUCCESS);
        }
    }
}
