/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

import net.kullo.libkullo.api.Address;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class AddressSet extends HashSet<Address> {

    public AddressSet() {
        super();
    }

    public AddressSet(Collection<Address> c) {
        super(c);
    }

    private static class AddressComparator implements Comparator<Address>
    {
        public int compare(Address lhs, Address rhs)
        {
            if (lhs == null || rhs == null) {
                throw new NullPointerException("Address must not be null");
            }

            return lhs.toString().compareTo(rhs.toString());
        }
    }

    public List<Address> sorted() {
        List<Address> list = new ArrayList<>(this);
        Collections.sort(list, new AddressComparator());
        return list;
    }
}
