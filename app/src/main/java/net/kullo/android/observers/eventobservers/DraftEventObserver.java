/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.observers.eventobservers;

import net.kullo.android.observers.EventObserver;

public interface DraftEventObserver extends EventObserver {
    void draftStateChanged(long conversationId);
    void draftTextChanged(long conversationId);
}
