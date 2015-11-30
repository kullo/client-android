/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.observers.eventobservers;

import net.kullo.android.observers.EventObserver;

public interface DraftAttachmentAddedEventObserver extends EventObserver {
    void draftAttachmentAdded(long conversationId, long attachmentId);
}
