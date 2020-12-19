/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
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
