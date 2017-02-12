/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.notifications;

import android.content.Intent;
import com.google.android.gms.iid.InstanceIDListenerService;

public class GcmInstanceIDListenerService extends InstanceIDListenerService {
    @Override
    public void onTokenRefresh() {
        // This is called by the InstanceID provider if the token is updated.
        // Pass the info to our registration service
        Intent intent = new Intent(this, GcmRegistrationIntentService.class);
        startService(intent);
    }
}
