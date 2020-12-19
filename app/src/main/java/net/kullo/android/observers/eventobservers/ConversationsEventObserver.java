/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
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
