/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.kulloapi;

import android.support.v7.widget.RecyclerView;

import net.kullo.javautils.RuntimeAssertion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * An adapter that stores a list of IDs.
 *
 * Note: Elements must be unique.
 *
 * @param <VH>
 */
public abstract class KulloIdsAdapter<VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {
    private ArrayList<Long> mItems = new ArrayList<>();
    private ArrayList<Long> mSelectedItems = new ArrayList<>();

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

    public void replaceAll(Collection<Long> collection) {
        RuntimeAssertion.require(collection != null);

        mSelectedItems.clear();
        mItems.clear();
        mItems.addAll(collection);
        notifyDataSetChanged();
    }

    public Long getItem(int position) {
        return mItems.get(position);
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

    /**
     * Looks for the position of a specific item. Returns -1 if not found.
     *
     * @param item an item be be looked for
     * @return the position
     */
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

    public void toggleSelectedItem(Long item) {
        if (mSelectedItems.contains(item)) {
            mSelectedItems.remove(item);
        } else {
            mSelectedItems.add(item);
        }

        int pos = mItems.indexOf(item);
        notifyItemChanged(pos);
    }

    public boolean isSelected(Long item) {
        return mSelectedItems.contains(item);
    }

    public int getSelectedItemsCount() {
        return mSelectedItems.size();
    }

    public boolean isSelectionActive() {
        return mSelectedItems.size() > 0;
    }

    public void clearSelectedItems() {
        for (Long item : mSelectedItems) {
            int pos = mItems.indexOf(item);
            notifyItemChanged(pos);
        }
        mSelectedItems.clear();
    }

    public List<Long> getSelectedItems() {
        return mSelectedItems;
    }

    public void notifyDataForIdChanged(long id) {
        notifyItemChanged(find(id));
    }

}
