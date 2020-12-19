/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.util.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * An adapter that stores a list of IDs.
 *
 * Note: Elements must be unique.
 *
 * @param <VH>
 */
public abstract class KulloIdsAdapter<VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH>
    implements ReadableAdapter<Long>, SelectableAdapter<Long> {
    @SuppressWarnings("unused") private static final String TAG = "KulloIdsAdapter";

    private ArrayList<Long> mItems = new ArrayList<>();
    private Set<Long> mSelectedItems = new HashSet<>();

    public KulloIdsAdapter() {
        setHasStableIds(true);
    }

    /**
     * Add newItem at the correct position
     *
     * @param newItem The item to be added
     * @param comparator A comparator used to place the new item in the right spot
     */
    public void add(Long newItem, Comparator<Long> comparator) {
        int insertionPosition = 0;
        while (insertionPosition < mItems.size()
                && comparator.compare(newItem, mItems.get(insertionPosition)) > 0) {
            // newItem > mItems[insertionPosition], so add it further down
            insertionPosition++;
        }

        mItems.add(insertionPosition, newItem);
        notifyItemInserted(insertionPosition);
    }

    public void replaceAll(@NonNull Collection<Long> collection) {
        mSelectedItems.clear();
        mItems.clear();
        mItems.addAll(collection);
        notifyDataSetChanged();
    }

    @Override
    public Long getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public Collection<Long> getItems() {
        return mItems;
    }

    public void append(Long newItem) {
        mItems.add(newItem);
        if (mItems.size() == 1) {
            // first item
            notifyDataSetChanged();
        } else {
            notifyItemInserted(mItems.size()-1);
        }
    }

    public void remove(Long item) {
        int pos = mItems.indexOf(item);
        if (pos != -1) {
            mItems.remove(pos);
            mSelectedItems.remove(item);
            notifyItemRemoved(pos);
        }
    }

    @Override
    public int find(Long item) {
        return mItems.indexOf(item);
    }

    @Override
    public long getItemId(int position) {
        // Since items is a list of long IDs anyway, this can be used directly
        return getItem(position);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public void toggleSelectedItem(Long item) {
        if (mSelectedItems.contains(item)) {
            mSelectedItems.remove(item);
        } else {
            mSelectedItems.add(item);
        }

        int pos = mItems.indexOf(item);
        notifyItemChanged(pos);
    }

    @Override
    public boolean isSelected(Long item) {
        return mSelectedItems.contains(item);
    }

    @Override
    public int getSelectedItemsCount() {
        return mSelectedItems.size();
    }

    @Override
    public boolean isSelectionActive() {
        return mSelectedItems.size() > 0;
    }

    @Override
    public void clearSelectedItems() {
        for (Long item : mSelectedItems) {
            int pos = find(item);
            notifyItemChanged(pos);
        }
        mSelectedItems.clear();
    }

    @Override
    public Set<Long> getSelectedItems() {
        return mSelectedItems;
    }

    /**
     * Tries to handle an element change. When the position of the given id does not change,
     * this method can handle it. Otherwise false is returned and the adapter must be refilled.
     */
    public boolean tryHandleElementChanged(long id, Comparator<Long> comparator) {
        boolean orderOfElementsChanged = false;

        // Let A <= B <= C be elements and assume B changed. Order has changed when
        // A > B or B > C

        @SuppressWarnings("UnnecessaryLocalVariable")
        long b = id;

        int bPos = find(b);
        int aPos = bPos-1;
        int cPos = bPos+1;

        if (aPos >= 0) {
            long a = mItems.get(aPos);
            orderOfElementsChanged = comparator.compare(a, b) > 0; // A > B
        }

        if (!orderOfElementsChanged && cPos < mItems.size()) {
            long c = mItems.get(cPos);
            orderOfElementsChanged = comparator.compare(b, c) > 0; // B > C
        }

        if (orderOfElementsChanged) {
            return false;
        } else {
            notifyItemChanged(bPos);
            return true;
        }
    }
}
