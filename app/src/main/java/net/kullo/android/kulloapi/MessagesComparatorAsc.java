/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.kulloapi;

import java.util.Comparator;

/**
 * Comparator for ascending sorting of messages based on id
 */
public class MessagesComparatorAsc implements Comparator<Long> {
    @Override
    public int compare(Long lhsMessageId, Long rhsMessageId) {
        return lhsMessageId.compareTo(rhsMessageId);
    }
}
