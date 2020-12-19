/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.util.adapters;

import java.util.Set;

public interface SelectableAdapter<ItemType> {

    void toggleSelectedItem(ItemType item);
    boolean isSelected(ItemType item);
    int getSelectedItemsCount();
    boolean isSelectionActive();
    void clearSelectedItems();
    Set<ItemType> getSelectedItems();
}
