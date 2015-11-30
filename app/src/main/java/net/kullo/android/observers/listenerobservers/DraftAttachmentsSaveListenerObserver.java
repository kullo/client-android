/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.observers.listenerobservers;

import net.kullo.android.observers.ListenerObserver;

public interface DraftAttachmentsSaveListenerObserver extends ListenerObserver {
    void finished(long messageId, long attachmentId, String path);
    void error(long messageId, long attachmentId, String path, String error);
}
