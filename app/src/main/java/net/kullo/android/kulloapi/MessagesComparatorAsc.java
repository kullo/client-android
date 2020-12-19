/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
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
