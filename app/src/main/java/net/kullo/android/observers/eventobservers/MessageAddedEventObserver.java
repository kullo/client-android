/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.observers.eventobservers;

import net.kullo.android.observers.EventObserver;

public interface MessageAddedEventObserver extends EventObserver {
    void messageAdded(long conversationId, long messageId);
}
