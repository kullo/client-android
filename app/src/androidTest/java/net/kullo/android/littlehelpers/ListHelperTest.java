/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

import android.test.AndroidTestCase;

import java.util.List;

public class ListHelperTest extends AndroidTestCase {
    public void testSimple() {
        {
            int inValue = 333;
            Integer expectedOutValue = 333;
            List actual = ListHelper.asList(inValue);
            assertEquals(actual.size(), 1);
            assertEquals(actual.get(0), expectedOutValue);
        }

        {
            long inValue = 333;
            Long expectedOutValue = 333L;
            List actual = ListHelper.asList(inValue);
            assertEquals(actual.size(), 1);
            assertEquals(actual.get(0), expectedOutValue);
        }

        {
            Long inValue = 333L;
            Long expectedOutValue = 333L;
            List actual = ListHelper.asList(inValue);
            assertEquals(actual.size(), 1);
            assertEquals(actual.get(0), expectedOutValue);
        }

        {
            String inValue = "foobar";
            String expectedOutValue = "foobar";
            List actual = ListHelper.asList(inValue);
            assertEquals(actual.size(), 1);
            assertEquals(actual.get(0), expectedOutValue);
        }
    }
}
