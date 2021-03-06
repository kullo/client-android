/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.littlehelpers;

import android.test.AndroidTestCase;

public class UriHelpersTest extends AndroidTestCase {
    public void testSimple() {
        assertEquals(UriHelpers.displayNameContainsExtension("abab"), false);
        assertEquals(UriHelpers.displayNameContainsExtension("a"), false);
        assertEquals(UriHelpers.displayNameContainsExtension("."), false);
        assertEquals(UriHelpers.displayNameContainsExtension("LOVE WILL KEEP US"), false);
        assertEquals(UriHelpers.displayNameContainsExtension("Moonbootica's December Mix"), false);

        assertEquals(UriHelpers.displayNameContainsExtension("image.jpg"), true);
        assertEquals(UriHelpers.displayNameContainsExtension("compilation_unit.o"), true);
        assertEquals(UriHelpers.displayNameContainsExtension(".htaccess"), true);
        assertEquals(UriHelpers.displayNameContainsExtension(".gitignore"), true);
        assertEquals(UriHelpers.displayNameContainsExtension("archive.xz"), true);
        assertEquals(UriHelpers.displayNameContainsExtension("archive.7z"), true);
    }
}
