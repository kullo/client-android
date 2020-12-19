/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.kulloapi;

import java.util.Comparator;

/**
 * A comparator that logs what it is doing.
 */
abstract public class CountingComparator implements Comparator<Long> {
    private int mCount = 0;

    CountingComparator() {
    }

    // To be called on a compare() implementation
    protected void count() {
        mCount++;
    }

    public int getCount() {
        return mCount;
    }

    public String getStats() {
        return "Comparisons: " + getCount();
    }
}
