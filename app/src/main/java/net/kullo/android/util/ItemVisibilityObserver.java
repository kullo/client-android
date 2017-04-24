/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.util;

import android.support.annotation.NonNull;

import org.joda.time.DateTime;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ItemVisibilityObserver<ItemType> {

    private final int thresholdTimeMs;
    private HashSet<ItemType> storage = new HashSet<>();

    // This may contain garbage values of elements no longer in `storage`,
    // so never use these keys
    private Map<ItemType, DateTime> insertionTimes = new HashMap<>();

    public ItemVisibilityObserver(int thresholdTimeMs) {
        this.thresholdTimeMs = thresholdTimeMs;
    }

    public void add(@NonNull ItemType item) {
        boolean added = storage.add(item);
        if (added) {
            insertionTimes.put(item, DateTime.now());
        }
    }

    public void remove(@NonNull ItemType item) {
        storage.remove(item);
        insertionTimes.remove(item);
    }

    public void removeAllExcept(@NonNull Set<ItemType> donNotRemove) {
        storage.retainAll(donNotRemove);
    }

    public void clear() {
        storage.clear();
        insertionTimes.clear();
    }

    @NonNull
    public Set<ItemType> getReadyItems() {
        final long nowMillis = DateTime.now().getMillis();

        Set<ItemType> out = new HashSet<>();
        for (ItemType item : storage) {
            if ((nowMillis-insertionTimes.get(item).getMillis()) >= thresholdTimeMs) {
                out.add(item);
            }
        }
        return out;
    }

    public void resetTimes() {
        resetTimesExcept(Collections.<ItemType>emptySet());
    }

    public void resetTimesExcept(@NonNull Set<ItemType> donNotReset) {
        DateTime now = DateTime.now();
        for (ItemType item : storage) {
            if (!donNotReset.contains(item)) {
                insertionTimes.put(item, now);
            }
        }
    }
}
