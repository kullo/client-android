/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.observers.listenerobservers;

import net.kullo.android.observers.ListenerObserver;

public interface ClientGenerateKeysListenerObserver extends ListenerObserver {
    void progress(byte progress);
    void finished();
}
