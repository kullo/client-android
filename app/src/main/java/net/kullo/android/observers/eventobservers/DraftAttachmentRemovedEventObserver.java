/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.observers.eventobservers;

import net.kullo.android.observers.EventObserver;

public interface DraftAttachmentRemovedEventObserver extends EventObserver {
    void draftAttachmentRemoved(long conversationId, long attachmentId);
}
