/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.kulloapi;

import net.kullo.libkullo.api.DateTime;
import net.kullo.libkullo.api.Session;

public class ConversationsComparatorDsc extends CountingComparator {
    private final Session mSession;

    // Note: session must be locked as long as this is used
    public ConversationsComparatorDsc(Session session) {
        super();
        mSession = session;
    }

    @Override
    public int compare(Long lhsConvId, Long rhsConvId) {
        count();
        DateTime lhsDate = mSession.conversations().latestMessageTimestamp(lhsConvId);
        DateTime rhsDate = mSession.conversations().latestMessageTimestamp(rhsConvId);
        return rhsDate.compareTo(lhsDate);
    }
}
