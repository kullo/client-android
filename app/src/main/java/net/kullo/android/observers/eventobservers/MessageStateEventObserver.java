/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.observers.eventobservers;

import net.kullo.android.observers.EventObserver;

public interface MessageStateEventObserver extends EventObserver {
    void messageStateChanged(long conversationId, long messageId);
}
