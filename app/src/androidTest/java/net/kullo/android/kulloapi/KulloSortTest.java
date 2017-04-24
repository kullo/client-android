/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.kulloapi;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class KulloSortTest extends TestCase {
    private final Comparator<Integer> INT_COMPARATOR = new Comparator<Integer>() {
        @Override
        public int compare(Integer lhs, Integer rhs) {
            if (lhs < rhs) return -1;
            if (lhs.equals(rhs)) return 0;
            return 1;
        }
    };

    public void testSortEmpty() throws Exception {
        List<Integer> empty = new ArrayList<>();
        KulloSort.sort(empty, INT_COMPARATOR);
        assertEquals(0, empty.size());
        assertEquals(new ArrayList<Integer>(), empty);
    }

    public void testSortSingle() throws Exception {
        List<Integer> single = new ArrayList<>(Collections.singletonList(1));
        KulloSort.sort(single, INT_COMPARATOR);
        assertEquals(1, single.size());
        assertEquals(Integer.valueOf(1), single.get(0));
    }

    public void testSortSorted() throws Exception {
        List<Integer> single = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        KulloSort.sort(single, INT_COMPARATOR);
        assertEquals(5, single.size());
        assertEquals(Integer.valueOf(1), single.get(0));
        assertEquals(Integer.valueOf(2), single.get(1));
        assertEquals(Integer.valueOf(3), single.get(2));
        assertEquals(Integer.valueOf(4), single.get(3));
        assertEquals(Integer.valueOf(5), single.get(4));
    }

    public void testSortReverse() throws Exception {
        List<Integer> single = new ArrayList<>(Arrays.asList(5, 4, 3, 2, 1));
        KulloSort.sort(single, INT_COMPARATOR);
        assertEquals(5, single.size());
        assertEquals(Integer.valueOf(1), single.get(0));
        assertEquals(Integer.valueOf(2), single.get(1));
        assertEquals(Integer.valueOf(3), single.get(2));
        assertEquals(Integer.valueOf(4), single.get(3));
        assertEquals(Integer.valueOf(5), single.get(4));
    }

    public void testSortRandom() throws Exception {
        List<Integer> single = new ArrayList<>(Arrays.asList(4, 2, 5, 1, 3));
        KulloSort.sort(single, INT_COMPARATOR);
        assertEquals(5, single.size());
        assertEquals(Integer.valueOf(1), single.get(0));
        assertEquals(Integer.valueOf(2), single.get(1));
        assertEquals(Integer.valueOf(3), single.get(2));
        assertEquals(Integer.valueOf(4), single.get(3));
        assertEquals(Integer.valueOf(5), single.get(4));
    }

    public void testSortEquals() throws Exception {
        List<Integer> single = new ArrayList<>(Arrays.asList(4, 4, 4, 4, 4));
        KulloSort.sort(single, INT_COMPARATOR);
        assertEquals(5, single.size());
        assertEquals(Integer.valueOf(4), single.get(0));
        assertEquals(Integer.valueOf(4), single.get(1));
        assertEquals(Integer.valueOf(4), single.get(2));
        assertEquals(Integer.valueOf(4), single.get(3));
        assertEquals(Integer.valueOf(4), single.get(4));
    }

    public void testSortLong() throws Exception {
        List<Integer> single = new ArrayList<>(Arrays.asList(4, 2, 5, 0, 12, 11, 7, 8, 10, 12));
        KulloSort.sort(single, INT_COMPARATOR);
        assertEquals(10, single.size());
        assertEquals(Integer.valueOf( 0), single.get(0));
        assertEquals(Integer.valueOf( 2), single.get(1));
        assertEquals(Integer.valueOf( 4), single.get(2));
        assertEquals(Integer.valueOf( 5), single.get(3));
        assertEquals(Integer.valueOf( 7), single.get(4));
        assertEquals(Integer.valueOf( 8), single.get(5));
        assertEquals(Integer.valueOf(10), single.get(6));
        assertEquals(Integer.valueOf(11), single.get(7));
        assertEquals(Integer.valueOf(12), single.get(8));
        assertEquals(Integer.valueOf(12), single.get(9));
    }

    public void testSortNegativeShort() throws Exception {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 0, -1));
        KulloSort.sort(list, INT_COMPARATOR);
        assertEquals(3, list.size());
        assertEquals(Integer.valueOf(-1), list.get(0));
        assertEquals(Integer.valueOf( 0), list.get(1));
        assertEquals(Integer.valueOf( 1), list.get(2));
    }

    public void testSortNegativeLong() throws Exception {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 0, -1, -9, -150, 150, 9));
        KulloSort.sort(list, INT_COMPARATOR);
        assertEquals(7, list.size());
        assertEquals(Integer.valueOf(-150), list.get(0));
        assertEquals(Integer.valueOf(  -9), list.get(1));
        assertEquals(Integer.valueOf(  -1), list.get(2));
        assertEquals(Integer.valueOf(   0), list.get(3));
        assertEquals(Integer.valueOf(   1), list.get(4));
        assertEquals(Integer.valueOf(   9), list.get(5));
        assertEquals(Integer.valueOf( 150), list.get(6));
    }
}
