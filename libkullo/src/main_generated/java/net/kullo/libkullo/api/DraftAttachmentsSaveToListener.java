// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from drafts.djinni

package net.kullo.libkullo.api;

/** Listener used in DraftAttachments.saveToAsync() */
public abstract class DraftAttachmentsSaveToListener {
    public abstract void finished(long convId, long attId, String path);

    public abstract void error(long convId, long attId, String path, LocalError error);
}