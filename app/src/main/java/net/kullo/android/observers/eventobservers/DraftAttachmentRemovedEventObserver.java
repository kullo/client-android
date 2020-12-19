/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.observers.eventobservers;

import net.kullo.android.observers.EventObserver;

public interface DraftAttachmentRemovedEventObserver extends EventObserver {
    void draftAttachmentRemoved(long conversationId, long attachmentId);
}
