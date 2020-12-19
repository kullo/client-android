/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.observers.listenerobservers;

import net.kullo.android.observers.ListenerObserver;

public interface MessageAttachmentsSaveListenerObserver extends ListenerObserver {
    void finished(long messageId, long attachmentId, String path);
    void error(long messageId, long attachmentId, String path, String error);
}
