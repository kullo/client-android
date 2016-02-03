/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.kulloapi;

import net.kullo.libkullo.api.DateTime;
import net.kullo.libkullo.api.Session;

/**
 * Comparator for ascending sorting of conversations based on latestMessageTime
 * Uses libkullo's DateTime compare functions directly. No Joda conversion needed.
 */
public class ConversationsComparatorAsc extends KulloComparator {
    public ConversationsComparatorAsc(Session session) {
        super(session);
    }

    @Override
    public int compare(Long lhsConvID, Long rhsConvID) {
        count();

        DateTime lhsDate = mSession.conversations().latestMessageTimestamp(lhsConvID);
        DateTime rhsDate = mSession.conversations().latestMessageTimestamp(rhsConvID);

        return lhsDate.compareTo(rhsDate);
    }
}
