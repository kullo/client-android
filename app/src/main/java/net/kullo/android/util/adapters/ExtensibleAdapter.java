/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.util.adapters;

public interface ExtensibleAdapter<ItemType> {
    /**
     * Tries to add an element into the adapter at a natural position
     *
     * If the adapter contains unique items, this may not add the new item.
     * In this case false is returned.
     *
     * @param newItem
     * @return true iff the element was added.
     */
    boolean add(ItemType newItem);

    /**
     * Tries to remove an element from the adapter. Only the first occurrence of
     * an element will be removed.
     *
     * @return true if an element was removed, false otherwise.
     */
    boolean remove(ItemType item);
}
