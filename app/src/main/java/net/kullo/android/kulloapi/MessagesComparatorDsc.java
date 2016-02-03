/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.kulloapi;

import net.kullo.libkullo.api.Session;

public class MessagesComparatorDsc extends MessagesComparatorAsc {
    public MessagesComparatorDsc(Session session) {
        super(session);
    }

    @Override
    public int compare(Long lhsMessageId, Long rhsMessageId) {
        return super.compare(lhsMessageId, rhsMessageId) * -1;
    }
}
