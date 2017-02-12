/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.kulloapi;

import android.support.annotation.NonNull;
import android.util.Log;

import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.Client;

public class ClientConnector {
    private static final String TAG = "ClientConnector";

    /* singleton setup */
    private static final ClientConnector SINGLETON = new ClientConnector();
    @NonNull public static ClientConnector get() {
        return SINGLETON;
    }

    /* members */
    private final Client mClient;

    private ClientConnector() {
        mClient = Client.create();
        Log.d(TAG, mClient.versions().toString());
    }

    @NonNull
    public Client getClient() {
        RuntimeAssertion.require(mClient != null);
        return mClient;
    }
}
