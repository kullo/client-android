/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.kulloapi;

import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.Session;

import java.util.Comparator;

/**
 * A comparator that logs what it is doing.
 */
abstract public class KulloComparator implements Comparator<Long> {
    private int mCount = 0;
    protected final Session mSession;

    KulloComparator(Session session) {
        RuntimeAssertion.require(session != null);
        mSession = session;
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
