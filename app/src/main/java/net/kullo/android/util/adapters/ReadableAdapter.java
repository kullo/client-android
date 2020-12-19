/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
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
