/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.kulloapi;

public class MessagesComparatorDsc extends MessagesComparatorAsc {
    @Override
    public int compare(Long lhsMessageId, Long rhsMessageId) {
        return super.compare(lhsMessageId, rhsMessageId) * -1;
    }
}
