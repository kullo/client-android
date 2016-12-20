/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.observers.listenerobservers;

import net.kullo.android.observers.ListenerObserver;
import net.kullo.libkullo.api.NetworkError;

public interface ClientCheckCredentialsListenerObserver extends ListenerObserver {
    void loginFailed();
    void error(NetworkError error);
}
