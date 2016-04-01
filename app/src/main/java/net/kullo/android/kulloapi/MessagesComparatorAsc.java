/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.kulloapi;

import net.kullo.libkullo.api.DateTime;
import net.kullo.libkullo.api.Session;

/**
 * Comparator for ascending sorting of messages based on dateReceived
 * Uses libkullo's DateTime compare functions directly. No Joda conversion needed.
 */
public class MessagesComparatorAsc extends KulloComparator {
    public MessagesComparatorAsc(Session session) {
        super(session);
    }

    @Override
    public int compare(Long lhsMessageId, Long rhsMessageId) {
        count();

        DateTime lhsDate = mSession.messages().dateReceived(lhsMessageId);
        DateTime rhsDate = mSession.messages().dateReceived(rhsMessageId);

        return lhsDate.compareTo(rhsDate);
    }
}
