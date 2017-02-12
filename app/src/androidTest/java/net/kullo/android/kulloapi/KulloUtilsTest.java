/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.kulloapi;

import android.test.AndroidTestCase;

import java.util.Arrays;
import java.util.List;

public class KulloUtilsTest extends AndroidTestCase {
    private static final List<String> DATA = Arrays.asList(
            // empty
            "", "",
            // simple
            "John", "J",
            "John Doe", "JD",
            // compound names
            "Foo Abc-Def", "FA",
            "Foo de Abc-Def", "FA",
            "Foo -Bar", "FB",
            // lower case
            "john", "J",
            "john doe", "JD",
            // limit length
            "John Mike Doe", "JD",
            "John Mike Doe Junior", "JJ",
            // numbers are okay
            "John 2", "J2",
            "1 2", "12",
            "4 All", "4A",
            // ignore special characters
            "John (Tablet)", "JT",
            "John _Tablet", "JT",
            "John 🐵 Doe", "JD",
            "John 🐵Doe", "JD",
            "🐵", "",
            // ignore empty parts
            "John ", "J",
            "John _", "J",
            // different whitespace
            "John  Doe", "JD",
            "John\tDoe", "JD",
            "John\nDoe", "JD",
            "John\rDoe", "JD",
            "John\t Doe", "JD",
            "John\n Doe", "JD",
            "John\r Doe", "JD",
            "John \tDoe", "JD",
            "John \nDoe", "JD",
            "John \r Doe", "JD",
            "John \t Doe", "JD",
            "John \n Doe", "JD",
            "John \r Doe", "JD",
            // Umlauts work
            "La Österreich", "LÖ",
            "ὕδωρ ῥυθμός", "ὝῬ",
            // Three bytes
            "朽", "朽",
            "A 朽", "A朽",
            "朽 A", "朽A",
            // Non-BMP character (U+01D49E)
            "\uD835\uDC9E", "\uD835\uDC9E",
            "A \uD835\uDC9E", "A\uD835\uDC9E",
            "\uD835\uDC9E A", "\uD835\uDC9EA"
    );

    public void testAll() {
        for (int i = 0; i < DATA.size(); i += 2) {
            String in = DATA.get(i);
            String expected = DATA.get(i + 1);
            assertEquals(expected, KulloUtils.generateInitialsForAddressAndName(in));
        }
    }
}
