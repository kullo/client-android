/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.observers.listenerobservers;

import net.kullo.android.observers.ListenerObserver;
import net.kullo.libkullo.api.LocalError;
import net.kullo.libkullo.api.NetworkError;

public interface ClientCreateSessionListenerObserver extends ListenerObserver {
    void finished();
    void loginFailed();
    void networkError(NetworkError error);
    void localError(LocalError error);
}
