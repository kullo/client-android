/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.kulloapi;

import net.kullo.javautils.Insertionsort;
import net.kullo.javautils.Quicksort;

import java.util.Comparator;
import java.util.List;

/**
 * A wrapper for a sorting algorithm that works for us
 */
public class KulloSort {
    // Good average
    public static <G> void sort(List<G> list, Comparator<G> comparator) {
        Quicksort.sort(list, comparator);
    }

    public static <G> void quicksort(List<G> list, Comparator<G> comparator) {
        Quicksort.sort(list, comparator);
    }

    // Good on pre-sorted data. Know what you do!
    public static <G> void insertionsort(List<G> list, Comparator<G> comparator) {
        Insertionsort.sort(list, comparator);
    }
}
