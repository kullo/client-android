/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.kulloapi;

import net.kullo.libkullo.api.Session;

public class ConversationsComparatorDsc extends ConversationsComparatorAsc {
    public ConversationsComparatorDsc(Session session) {
        super(session);
    }

    @Override
    public int compare(Long lhsConvID, Long rhsConvID) {
        return super.compare(lhsConvID, rhsConvID) * -1;
    }
}
