/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.util.adapters;

import android.support.v7.widget.RecyclerView;

import net.kullo.android.littlehelpers.AddressSet;
import net.kullo.libkullo.api.Address;

import java.util.Set;

public abstract class AddressesAdapter<VH extends RecyclerView.ViewHolder>
    extends RecyclerView.Adapter<VH>
    implements ReadableAdapter<Address>, ExtensibleAdapter<Address>, SelectableAdapter<Address> {

    final private AddressSet storage;
    final private Set<Address> selectedAddresses = new AddressSet();

    protected AddressesAdapter(AddressSet set) {
        storage = set;
    }

    @Override
    public Address getItem(int pos) {
        return storage.sorted().get(pos);
    }

    @Override
    public int find(Address item) {
        return storage.sorted().indexOf(item);
    }

    @Override
    public AddressSet getItems() {
        return storage;
    }

    @Override
    public boolean add(Address address) {
        boolean somethingChanged = storage.add(address);
        if (somethingChanged) notifyDataSetChanged();
        return somethingChanged;
    }

    @Override
    public boolean remove(Address item) {
        int pos = find(item);
        if (pos != -1) {
            selectedAddresses.remove(item);
            storage.remove(item);
            notifyItemRemoved(pos);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getItemCount() {
        return storage.size();
    }

    @Override
    public void toggleSelectedItem(Address item) {
        if (selectedAddresses.contains(item)) {
            selectedAddresses.remove(item);
        } else {
            selectedAddresses.add(item);
        }

        int pos = find(item);
        notifyItemChanged(pos);
    }

    @Override
    public boolean isSelected(Address item) {
        return selectedAddresses.contains(item);
    }

    @Override
    public int getSelectedItemsCount() {
        return selectedAddresses.size();
    }

    @Override
    public boolean isSelectionActive() {
        return selectedAddresses.size() > 0;
    }

    @Override
    public void clearSelectedItems() {
        for (Address item : selectedAddresses) {
            int pos = find(item);
            notifyItemChanged(pos);
        }
        selectedAddresses.clear();
    }

    @Override
    public Set<Address> getSelectedItems() {
        return selectedAddresses;
    }
}
