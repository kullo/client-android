/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.notifications;

import com.google.firebase.iid.FirebaseInstanceIdService;

import net.kullo.android.kulloapi.SessionConnector;

public class InstanceIdService extends FirebaseInstanceIdService {
    @Override
    public void onTokenRefresh() {
        if (!SessionConnector.get().sessionAvailable()) {
            // Google Play Services might relaunch this when token changed
            // while the session was inactive. Ignore this case.
            return;
        }

        GcmConnector.get().getAndRegisterTokenAsync();
    }
}
