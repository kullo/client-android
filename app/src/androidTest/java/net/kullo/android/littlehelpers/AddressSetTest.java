/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

import android.support.test.runner.AndroidJUnit4;

import net.kullo.libkullo.LibKullo;
import net.kullo.libkullo.api.Address;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class AddressSetTest {

    @BeforeClass
    public static void setUp() {
        LibKullo.init();
    }

    @Test
    public void defaultConstructor() {
        final AddressSet empty = new AddressSet();
        assertEquals(empty.size(), 0);
    }

    @Test
    public void add() {
        AddressSet set = new AddressSet();
        set.add(Address.create("test#kullo.net"));

        assertEquals(1, set.size());
        assertTrue(set.contains(Address.create("test#kullo.net")));
    }

    @Test
    public void addNull() {
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

    @Test
    public void addInvalidAddress() {
        AddressSet set = new AddressSet();
        set.add(Address.create("bs string"));

        assertEquals(1, set.size());
        assertTrue(set.contains(null));
    }

    @Test
    public void contains() {
        AddressSet set = new AddressSet();
        set.add(Address.create("test#kullo.net"));

        assertTrue(set.contains(Address.create("test#kullo.net")));
        assertFalse(set.contains(Address.create("different#kullo.net")));
    }

    @Test
    public void containsOtherType() {
        AddressSet set = new AddressSet();
        set.add(Address.create("test#kullo.net"));

        String searchAddress = "test#kullo.net";
        assertFalse(set.contains(searchAddress));
    }

    @Test
    public void addMultipleTimes() {
        AddressSet set = new AddressSet();
        set.add(Address.create("test#kullo.net"));
        set.add(Address.create("test#kullo.net"));

        assertEquals(1, set.size());
        assertTrue(set.contains(Address.create("test#kullo.net")));
    }

    @Test
    public void remove() {
        AddressSet set = new AddressSet();
        set.add(Address.create("test#kullo.net"));

        assertEquals(1, set.size());
        assertTrue(set.contains(Address.create("test#kullo.net")));

        set.remove(Address.create("test#kullo.net"));

        assertEquals(0, set.size());
        assertFalse(set.contains(Address.create("test#kullo.net")));
    }

    @Test
    public void removeNull() {
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

    @Test
    public void removeNonExisting() {
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

    @Test
    public void removeMultipleElementsRemoveFromFront() {
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

    @Test
    public void removeMultipleElementsRemoveFromBack() {
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
    @Test
    public void removeOtherType() {
        AddressSet set = new AddressSet();
        set.add(Address.create("test#kullo.net"));

        String searchAddress = "test#kullo.net";
        set.remove(searchAddress);

        assertEquals(1, set.size());
        assertTrue(set.contains(Address.create("test#kullo.net")));
    }

    @Test
    public void sorted() {
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
    @Test
    public void sortedNull() {
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
