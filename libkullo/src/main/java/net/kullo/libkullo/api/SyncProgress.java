// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from syncer.djinni

package net.kullo.libkullo.api;

/** Encodes a progress update during syncing. Unknown totals are set to 0. */
public final class SyncProgress {


    /*package*/ final long incomingMessagesProcessed;

    /*package*/ final long incomingMessagesTotal;

    /*package*/ final long incomingMessagesNew;

    /*package*/ final long incomingMessagesNewUnread;

    /*package*/ final long incomingMessagesModified;

    /*package*/ final long incomingMessagesDeleted;

    /*package*/ final long incomingAttachmentsDownloadedBytes;

    /*package*/ final long incomingAttachmentsTotalBytes;

    /*package*/ final long outgoingMessagesUploadedBytes;

    /*package*/ final long outgoingMessagesTotalBytes;

    /*package*/ final long runTimeMs;

    public SyncProgress(
            long incomingMessagesProcessed,
            long incomingMessagesTotal,
            long incomingMessagesNew,
            long incomingMessagesNewUnread,
            long incomingMessagesModified,
            long incomingMessagesDeleted,
            long incomingAttachmentsDownloadedBytes,
            long incomingAttachmentsTotalBytes,
            long outgoingMessagesUploadedBytes,
            long outgoingMessagesTotalBytes,
            long runTimeMs) {
        this.incomingMessagesProcessed = incomingMessagesProcessed;
        this.incomingMessagesTotal = incomingMessagesTotal;
        this.incomingMessagesNew = incomingMessagesNew;
        this.incomingMessagesNewUnread = incomingMessagesNewUnread;
        this.incomingMessagesModified = incomingMessagesModified;
        this.incomingMessagesDeleted = incomingMessagesDeleted;
        this.incomingAttachmentsDownloadedBytes = incomingAttachmentsDownloadedBytes;
        this.incomingAttachmentsTotalBytes = incomingAttachmentsTotalBytes;
        this.outgoingMessagesUploadedBytes = outgoingMessagesUploadedBytes;
        this.outgoingMessagesTotalBytes = outgoingMessagesTotalBytes;
        this.runTimeMs = runTimeMs;
    }

    /** inbox (unit: messages) */
    public long getIncomingMessagesProcessed() {
        return incomingMessagesProcessed;
    }

    public long getIncomingMessagesTotal() {
        return incomingMessagesTotal;
    }

    public long getIncomingMessagesNew() {
        return incomingMessagesNew;
    }

    public long getIncomingMessagesNewUnread() {
        return incomingMessagesNewUnread;
    }

    public long getIncomingMessagesModified() {
        return incomingMessagesModified;
    }

    public long getIncomingMessagesDeleted() {
        return incomingMessagesDeleted;
    }

    /** incoming attachments (unit: bytes) */
    public long getIncomingAttachmentsDownloadedBytes() {
        return incomingAttachmentsDownloadedBytes;
    }

    public long getIncomingAttachmentsTotalBytes() {
        return incomingAttachmentsTotalBytes;
    }

    /** outgoing messages + attachments (unit: uncompressed bytes) */
    public long getOutgoingMessagesUploadedBytes() {
        return outgoingMessagesUploadedBytes;
    }

    public long getOutgoingMessagesTotalBytes() {
        return outgoingMessagesTotalBytes;
    }

    /** Run time of the current sync (unit: milliseconds) */
    public long getRunTimeMs() {
        return runTimeMs;
    }

    @Override
    public String toString() {
        return "SyncProgress{" +
                "incomingMessagesProcessed=" + incomingMessagesProcessed +
                "," + "incomingMessagesTotal=" + incomingMessagesTotal +
                "," + "incomingMessagesNew=" + incomingMessagesNew +
                "," + "incomingMessagesNewUnread=" + incomingMessagesNewUnread +
                "," + "incomingMessagesModified=" + incomingMessagesModified +
                "," + "incomingMessagesDeleted=" + incomingMessagesDeleted +
                "," + "incomingAttachmentsDownloadedBytes=" + incomingAttachmentsDownloadedBytes +
                "," + "incomingAttachmentsTotalBytes=" + incomingAttachmentsTotalBytes +
                "," + "outgoingMessagesUploadedBytes=" + outgoingMessagesUploadedBytes +
                "," + "outgoingMessagesTotalBytes=" + outgoingMessagesTotalBytes +
                "," + "runTimeMs=" + runTimeMs +
        "}";
    }

}
