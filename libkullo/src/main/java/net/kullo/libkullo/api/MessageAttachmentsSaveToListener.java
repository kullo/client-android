// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from api.djinni

package net.kullo.libkullo.api;

/** Listener used in MessageAttachments.saveToAsync() */
public abstract class MessageAttachmentsSaveToListener {
    public abstract void finished(long msgId, long attId, String path);

    public abstract void error(long msgId, long attId, String path, LocalError error);
}
