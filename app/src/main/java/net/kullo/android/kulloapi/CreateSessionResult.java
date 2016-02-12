/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.kulloapi;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.kullo.libkullo.api.AsyncTask;

public class CreateSessionResult {
    @NonNull
    public CreateSessionState state;

    @Nullable
    public AsyncTask task = null;

    public CreateSessionResult(CreateSessionState pState) {
        state = pState;
    }

    public CreateSessionResult(CreateSessionState pState, AsyncTask pTask) {
        state = pState;
        task = pTask;
    }
}
