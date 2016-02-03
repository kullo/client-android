/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

import net.kullo.libkullo.api.Address;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * Created by simon on 01.09.15.
 */
public class AddressSet extends HashSet<Address> {

    public AddressSet() {
        super();
    }

    public AddressSet(Collection<Address> c) {
        super(c);
    }

    @Override
    public boolean contains(Object obj) {
        Address objAddress = null;

        try {
            objAddress = (Address) obj;
        } catch (ClassCastException ex) {
            // obj is not of type Address => obj is not in set
            return false;
        }

        for (Address a : this) {
            if ((a == null && obj == null) || (a != null && objAddress != null && a.isEqualTo(objAddress))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean add(Address obj) {
        if (contains(obj)) {
            return false;
        }
        else {
            super.add(obj);
            return true;
        }
    }

    @Override
    public boolean remove(Object obj) {
        Address objAddress = null;

        Throwable e = null;
        try {
            objAddress = (Address) obj;
        } catch (ClassCastException ex) {
            // obj is not of type Address => obj is not in set
            return false;
        }

        for (Address a : this) {
            if ((a == null && objAddress == null) || (a != null && objAddress != null && a.isEqualTo(objAddress))) {
                super.remove(a);
                return true;
            }
        }

        return false;
    }

    static class AddressComparator implements Comparator<Address>
    {
        // < 0 if lhs is less than rhs
        // 0 if they are equal
        // > 0 if lhs is greater than rhs
        //
        // Make sure that null is the last element => null is greater than everything
        // null > a
        // b < null
        public int compare(Address lhs, Address rhs)
        {
            if (lhs == null && rhs == null) return 0;
            if (lhs == null) return 1;
            if (rhs == null) return -1;

            // both are not-null
            if (lhs == rhs) return 0;
            if (lhs.isLessThan(rhs)) return -1;
            return 1;
        }
    }

    public List<Address> sorted() {
        if (this == null) return null;

        List<Address> list = new ArrayList<Address>(this);

        Collections.sort(list, new AddressComparator());

        return list;

    }
}
