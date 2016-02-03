/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.observers.listenerobservers;

import net.kullo.android.observers.ListenerObserver;
import net.kullo.libkullo.api.NetworkError;
import net.kullo.libkullo.api.SyncProgress;

public interface SyncerListenerObserver extends ListenerObserver {
    void started();
    void draftAttachmentsTooBig(long convId);
    void progressed(SyncProgress progress);
    void finished();
    void error(NetworkError error);
}
