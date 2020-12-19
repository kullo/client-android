/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
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
