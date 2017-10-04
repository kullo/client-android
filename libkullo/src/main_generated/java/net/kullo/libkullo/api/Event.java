// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from event.djinni

package net.kullo.libkullo.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public final class Event {


    /*package*/ final EventType event;

    /*package*/ final long conversationId;

    /*package*/ final long messageId;

    /*package*/ final long attachmentId;

    public Event(
            @NonNull EventType event,
            long conversationId,
            long messageId,
            long attachmentId) {
        this.event = event;
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.attachmentId = attachmentId;
    }

    @NonNull
    public EventType getEvent() {
        return event;
    }

    /** -1 if not applicable */
    public long getConversationId() {
        return conversationId;
    }

    /** -1 if not applicable */
    public long getMessageId() {
        return messageId;
    }

    /** -1 if not applicable */
    public long getAttachmentId() {
        return attachmentId;
    }

    @Override
    public String toString() {
        return "Event{" +
                "event=" + event +
                "," + "conversationId=" + conversationId +
                "," + "messageId=" + messageId +
                "," + "attachmentId=" + attachmentId +
        "}";
    }

}
