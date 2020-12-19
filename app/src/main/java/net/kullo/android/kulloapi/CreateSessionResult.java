/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
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
