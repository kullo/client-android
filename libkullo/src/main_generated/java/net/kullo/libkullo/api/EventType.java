// AUTOGENERATED FILE - DO NOT MODIFY!
// This file generated by Djinni from event.djinni

package net.kullo.libkullo.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public enum EventType {
    /** Conversation added */
    CONVERSATIONADDED,
    /** Conversation removed */
    CONVERSATIONREMOVED,
    /** Any property of Conversations has changed for a single conversation */
    CONVERSATIONCHANGED,
    /**
     * Timestamp of the latest message in a single conversation changed. This may affect
     * the order of the conversations list. Whenever this is emitted, ConversationChanged
     * is also emitted.
     */
    CONVERSATIONLATESTMESSAGETIMESTAMPCHANGED,
    DRAFTSTATECHANGED,
    DRAFTTEXTCHANGED,
    DRAFTATTACHMENTADDED,
    DRAFTATTACHMENTREMOVED,
    MESSAGEADDED,
    MESSAGEDELIVERYCHANGED,
    MESSAGESTATECHANGED,
    MESSAGEATTACHMENTSDOWNLOADEDCHANGED,
    MESSAGEREMOVED,
    /**
     * One of the user settings keys changed. Emitted once for every key that changed.
     * TODO: pass key with this event
     */
    USERSETTINGSCHANGED,
    ;
}
