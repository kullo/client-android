/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

import android.test.AndroidTestCase;

import net.kullo.libkullo.LibKullo;
import net.kullo.libkullo.api.Address;

import java.util.List;


/**
 * Created by simon on 01.09.15.
 */
public class AddressSetTest extends AndroidTestCase {

    public void setUp() throws Exception {
        super.setUp();
        LibKullo.init();
    }

    public void testDefaultConstructor() {
        AddressSet empty = new AddressSet();
        assertEquals(empty.size(), 0);
    }

    public void testAdd() {
        AddressSet set = new AddressSet();
        set.add(Address.create("test#kullo.net"));

        assertEquals(1, set.size());
        assertTrue(set.contains(Address.create("test#kullo.net")));
    }

    public void testAddNull() {
        AddressSet set = new AddressSet();
        set.add(null);

        assertEquals(1, set.size());
        assertTrue(set.contains(null));

        // Add multiple times
        set.add(null);
        set.add(null);

        assertEquals(1, set.size());
        assertTrue(set.contains(null));
    }

    public void testAddInvalidAddress() {
        AddressSet set = new AddressSet();
        set.add(Address.create("bs string"));

        assertEquals(1, set.size());
        assertTrue(set.contains(null));
    }

    public void testContains() {
        AddressSet set = new AddressSet();
        set.add(Address.create("test#kullo.net"));

        assertTrue(set.contains(Address.create("test#kullo.net")));
        assertFalse(set.contains(Address.create("different#kullo.net")));
    }

    public void testContainsOtherType() {
        AddressSet set = new AddressSet();
        set.add(Address.create("test#kullo.net"));

        String searchAddress = "test#kullo.net";
        assertFalse(set.contains(searchAddress));
    }

    public void testAddMultipleTimes() {
        AddressSet set = new AddressSet();
        set.add(Address.create("test#kullo.net"));
        set.add(Address.create("test#kullo.net"));

        assertEquals(1, set.size());
        assertTrue(set.contains(Address.create("test#kullo.net")));
    }

    public void testRemove() {
        AddressSet set = new AddressSet();
        set.add(Address.create("test#kullo.net"));

        assertEquals(1, set.size());
        assertTrue(set.contains(Address.create("test#kullo.net")));

        set.remove(Address.create("test#kullo.net"));

        assertEquals(0, set.size());
        assertFalse(set.contains(Address.create("test#kullo.net")));
    }

    public void testRemoveNull() {
        AddressSet set = new AddressSet();
        set.add(Address.create("a#kullo.net"));
        set.add(null);
        set.add(Address.create("b#kullo.net"));

        assertEquals(3, set.size());

        set.remove(null);

        assertEquals(2, set.size());
        assertTrue(set.contains(Address.create("a#kullo.net")));
        assertTrue(set.contains(Address.create("b#kullo.net")));
    }

    public void testRemoveNonExisting() {
        AddressSet set = new AddressSet();
        set.add(Address.create("test#kullo.net"));

        assertEquals(1, set.size());
        assertTrue(set.contains(Address.create("test#kullo.net")));

        set.remove(Address.create("test#kullo.net"));

        assertEquals(0, set.size());
        assertFalse(set.contains(Address.create("test#kullo.net")));

        // Remove again
        set.remove(Address.create("test#kullo.net"));

        assertEquals(0, set.size());
        assertFalse(set.contains(Address.create("test#kullo.net")));
    }

    public void testRemoveMultipleElementsRemoveFromFront() {
        AddressSet set = new AddressSet();
        set.add(Address.create("a#kullo.net"));
        set.add(Address.create("b#kullo.net"));
        set.add(Address.create("c#kullo.net"));

        assertEquals(3, set.size());
        assertTrue(set.contains(Address.create("a#kullo.net")));
        assertTrue(set.contains(Address.create("b#kullo.net")));
        assertTrue(set.contains(Address.create("c#kullo.net")));

        set.remove(Address.create("a#kullo.net"));

        assertEquals(2, set.size());
        assertTrue(set.contains(Address.create("b#kullo.net")));
        assertTrue(set.contains(Address.create("c#kullo.net")));

        set.remove(Address.create("b#kullo.net"));

        assertEquals(1, set.size());
        assertTrue(set.contains(Address.create("c#kullo.net")));

        set.remove(Address.create("c#kullo.net"));

        assertEquals(0, set.size());
        assertFalse(set.contains(Address.create("a#kullo.net")));
        assertFalse(set.contains(Address.create("b#kullo.net")));
        assertFalse(set.contains(Address.create("c#kullo.net")));
    }

    public void testRemoveMultipleElementsRemoveFromBack() {
        AddressSet set = new AddressSet();
        set.add(Address.create("a#kullo.net"));
        set.add(Address.create("b#kullo.net"));
        set.add(Address.create("c#kullo.net"));

        assertEquals(3, set.size());
        assertTrue(set.contains(Address.create("a#kullo.net")));
        assertTrue(set.contains(Address.create("b#kullo.net")));
        assertTrue(set.contains(Address.create("c#kullo.net")));

        set.remove(Address.create("c#kullo.net"));

        assertEquals(2, set.size());
        assertTrue(set.contains(Address.create("a#kullo.net")));
        assertTrue(set.contains(Address.create("b#kullo.net")));

        set.remove(Address.create("b#kullo.net"));

        assertEquals(1, set.size());
        assertTrue(set.contains(Address.create("a#kullo.net")));

        set.remove(Address.create("a#kullo.net"));

        assertEquals(0, set.size());
        assertFalse(set.contains(Address.create("a#kullo.net")));
        assertFalse(set.contains(Address.create("b#kullo.net")));
        assertFalse(set.contains(Address.create("c#kullo.net")));
    }

    // Removing object of wrong type does not change set because set cannot contain object
    public void testRemoveOtherType() {
        AddressSet set = new AddressSet();
        set.add(Address.create("test#kullo.net"));

        String searchAddress = "test#kullo.net";
        set.remove(searchAddress);

        assertEquals(1, set.size());
        assertTrue(set.contains(Address.create("test#kullo.net")));
    }

    public void testSorted() {
        AddressSet set = new AddressSet();
        set.add(Address.create("a#kullo.net"));
        set.add(Address.create("c#kullo.net"));
        set.add(Address.create("b#kullo.net"));

        List<Address> sorted = set.sorted();
        assertTrue(sorted.get(0).isEqualTo(Address.create("a#kullo.net")));
        assertTrue(sorted.get(1).isEqualTo(Address.create("b#kullo.net")));
        assertTrue(sorted.get(2).isEqualTo(Address.create("c#kullo.net")));
    }

    // Let null be the last element
    public void testSortedNull() {
        AddressSet set = new AddressSet();
        set.add(Address.create("a#kullo.net"));
        set.add(null);
        set.add(Address.create("c#kullo.net"));
        set.add(Address.create("b#kullo.net"));

        List<Address> sorted = set.sorted();
        assertTrue(sorted.get(0).isEqualTo(Address.create("a#kullo.net")));
        assertTrue(sorted.get(1).isEqualTo(Address.create("b#kullo.net")));
        assertTrue(sorted.get(2).isEqualTo(Address.create("c#kullo.net")));
        assertTrue(sorted.get(3) == null);
    }

}
