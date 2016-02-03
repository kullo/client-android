/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.observers.listenerobservers;

import net.kullo.android.observers.ListenerObserver;

public interface DraftAttachmentsAddListenerObserver extends ListenerObserver {
    void finished(long convId, long attId, String path);
    void error(String error);
}
