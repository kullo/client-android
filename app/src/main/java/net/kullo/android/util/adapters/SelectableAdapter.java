/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
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
