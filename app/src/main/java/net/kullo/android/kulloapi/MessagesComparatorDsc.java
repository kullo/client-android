/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.kulloapi;

public class MessagesComparatorDsc extends MessagesComparatorAsc {
    @Override
    public int compare(Long lhsMessageId, Long rhsMessageId) {
        return super.compare(lhsMessageId, rhsMessageId) * -1;
    }
}
