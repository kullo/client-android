/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
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
