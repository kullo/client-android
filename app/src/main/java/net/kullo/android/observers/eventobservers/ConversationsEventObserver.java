/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.observers.eventobservers;

import net.kullo.android.observers.EventObserver;

public interface ConversationsEventObserver extends EventObserver {
    void conversationAdded(long conversationId);
    void conversationChanged(long conversationId);
    void conversationRemoved(long conversationId);
}
