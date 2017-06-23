/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.notifications;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.AnyThread;
import android.support.annotation.CheckResult;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.iid.FirebaseInstanceId;

import net.kullo.android.application.KulloApplication;
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

    // non-final members, access from synchronized methods only
    @Nullable private Boolean mGooglePlayAvailable = null;
    @Nullable private NotAvailableReason mGooglePlayNotAvailableReason = null;

    // check gplay at app start, prompt user if not there
    @CheckResult
    @Nullable
    synchronized public Dialog checkGooglePlayAvailabilityAndPrompt(Activity activity) {
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
            mGooglePlayAvailable = true;
            return null;
        }
    }

    @AnyThread
    synchronized public boolean googlePlayAvailable() {
        if (mGooglePlayAvailable == null) {
            GoogleApiAvailability playApi = GoogleApiAvailability.getInstance();
            int result = playApi.isGooglePlayServicesAvailable(KulloApplication.sharedInstance);
            mGooglePlayAvailable = (result == ConnectionResult.SUCCESS);
        }

        return mGooglePlayAvailable;
    }

    @AnyThread
    synchronized public NotAvailableReason googlePlayNotAvailableReason() {
        return mGooglePlayNotAvailableReason;
    }

    @MainThread
    public void ensureSessionHasTokenRegisteredAsync() {
        RuntimeAssertion.require(SessionConnector.get().sessionAvailable());

        // check if session already has a valid token, if it does return
        // if the token changes, GcmInstanceIdListenerService will notify the token service
        if (SessionConnector.get().hasPushToken()) {
            return;
        }

        if (!googlePlayAvailable()) {
            Log.i(TAG, "Device does not have Google Play Services. Skipping Push notifications.");
            return;
        }

        getAndRegisterTokenAsync();
    }

    @AnyThread
    void getAndRegisterTokenAsync() {
        RuntimeAssertion.require(googlePlayAvailable());

        final String newPushToken = FirebaseInstanceId.getInstance().getToken();
        Log.i(TAG, "Push token: " + newPushToken);

        if (newPushToken == null) return;

        new Handler(KulloApplication.sharedInstance.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                SessionConnector.get().tryUnregisterPushTokenAsync(10_000, new SessionConnector.UnregisterPushTokenCallback() {
                    @Override
                    public void onDone(boolean success) {
                        SessionConnector.get().registerPushTokenAsync(newPushToken);
                    }
                });
            }
        });
    }

    public void removeAllNotifications(Context context) {
        ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
    }
}
