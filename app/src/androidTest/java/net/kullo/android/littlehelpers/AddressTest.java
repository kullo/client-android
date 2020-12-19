/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.littlehelpers;

import android.support.test.runner.AndroidJUnit4;

import net.kullo.libkullo.api.AddressHelpers;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(AndroidJUnit4.class)
public class AddressTest {
    @Test
    public void equalsWorks() {
        assertEquals(AddressHelpers.create("b#example.com"), AddressHelpers.create("b#example.com"));
        assertEquals(AddressHelpers.create("B#example.com"), AddressHelpers.create("b#example.com"));
        assertEquals(AddressHelpers.create("b#EXAMPLE.com"), AddressHelpers.create("b#example.com"));

        assertNotEquals(AddressHelpers.create("be#example.com"), AddressHelpers.create("b#example.com"));
        assertNotEquals(AddressHelpers.create("b#example.org"), AddressHelpers.create("b#example.com"));

        assertNotEquals(AddressHelpers.create("be#example.com"), null);
        assertNotEquals(AddressHelpers.create("b#example.org"), null);

        // different type
        assertNotEquals(AddressHelpers.create("be#example.com"), "be#example.com");
        assertNotEquals(AddressHelpers.create("b#example.org"), "b#example.org");
    }

    @Test
    public void hashCodeWorks() {
        assertEquals(AddressHelpers.create("b#example.com").hashCode(), AddressHelpers.create("b#example.com").hashCode());
        assertEquals(AddressHelpers.create("B#example.com").hashCode(), AddressHelpers.create("b#example.com").hashCode());
        assertEquals(AddressHelpers.create("b#EXAMPLE.com").hashCode(), AddressHelpers.create("b#example.com").hashCode());

        assertNotEquals(AddressHelpers.create("be#example.com").hashCode(), AddressHelpers.create("b#example.com").hashCode());
        assertNotEquals(AddressHelpers.create("b#example.org").hashCode(), AddressHelpers.create("b#example.com").hashCode());
    }
}
