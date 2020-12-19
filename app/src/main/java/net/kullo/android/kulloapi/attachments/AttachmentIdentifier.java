/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.kulloapi.attachments;

import android.net.Uri;

import java.util.Locale;

public class AttachmentIdentifier {
    public long messageId;
    public long attachmentId;

    public Uri asUri() {
        return Uri.parse(
            "incomingAttachments://" +
            "inbox/" + messageId + "/" + attachmentId);
    }

    @Override
    public String toString() {
        return asUri().toString();
    }
}
