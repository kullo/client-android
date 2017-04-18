/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.observers.eventobservers;

import android.support.annotation.MainThread;

import net.kullo.android.observers.EventObserver;

public interface ConversationsEventObserver extends EventObserver {
    @MainThread
    void conversationAdded(long conversationId);

    @MainThread
    void conversationChanged(long conversationId);

    @MainThread
    void conversationRemoved(long conversationId);
}
