/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.observers.eventobservers;

import android.support.annotation.MainThread;

import net.kullo.android.observers.EventObserver;

public interface MessageAttachmentsDownloadedChangedEventObserver extends EventObserver {
    @MainThread
    void messageAttachmentsDownloadedChanged(long messageId);
}
