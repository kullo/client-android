/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.observers.listenerobservers;

import net.kullo.android.observers.ListenerObserver;
import net.kullo.libkullo.api.LocalError;
import net.kullo.libkullo.api.NetworkError;

public interface ClientCreateSessionListenerObserver extends ListenerObserver {
    void migrationStarted();
    void finished();
    void error(LocalError error);
}
