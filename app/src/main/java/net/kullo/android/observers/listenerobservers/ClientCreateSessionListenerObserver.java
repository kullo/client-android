/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.observers.listenerobservers;

import net.kullo.android.observers.ListenerObserver;

public interface ClientCreateSessionListenerObserver extends ListenerObserver {
    void finished();
    void error(String error);
}
