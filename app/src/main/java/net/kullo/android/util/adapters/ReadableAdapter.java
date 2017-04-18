/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.util.adapters;

import java.util.Collection;

public interface ReadableAdapter<ItemType> {
    /**
     * Looks for the position of a specific item. Returns -1 if not found.
     *
     * @param item an item be be looked for
     * @return the position
     */
    int find(ItemType item);
    ItemType getItem(int position);
    Collection<ItemType> getItems();
}
