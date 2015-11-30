/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.kulloapi;

import net.kullo.libkullo.api.DateTime;
import net.kullo.libkullo.api.Session;

/**
 * Comparator for ascending sorting of messages based on dateSent
 * Uses libkullo's DateTime compare functions directly. No Joda conversion needed.
 */
public class MessagesComparatorAsc extends KulloComparator {
    public MessagesComparatorAsc(Session session) {
        super(session);
    }

    @Override
    public int compare(Long lhsMessageId, Long rhsMessageId) {
        count();

        DateTime lhsDate = mSession.messages().dateSent(lhsMessageId);
        DateTime rhsDate = mSession.messages().dateSent(rhsMessageId);

        return lhsDate.compareTo(rhsDate);
    }
}
