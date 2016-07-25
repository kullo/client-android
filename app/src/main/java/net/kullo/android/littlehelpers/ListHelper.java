/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

import java.util.ArrayList;
import java.util.List;

public class ListHelper {
    public static <T> List<T> asList(T element) {
        ArrayList<T> out = new ArrayList<T>(1 /*initialCapacity*/);
        out.add(element);
        return out;
    }
}
