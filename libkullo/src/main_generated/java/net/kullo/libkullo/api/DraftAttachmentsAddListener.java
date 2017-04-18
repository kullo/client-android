// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from drafts.djinni

package net.kullo.libkullo.api;

/** Listener used in DraftAttachments.addAsync() */
public abstract class DraftAttachmentsAddListener {
    /**
     * Indicates progress when adding an attachment.
     * Note: not guaranteed to be called at any specific point.
     */
    public abstract void progressed(long convId, long attId, long bytesProcessed, long bytesTotal);

    public abstract void finished(long convId, long attId, String path);

    public abstract void error(long convId, String path, LocalError error);
}