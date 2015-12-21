/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.kulloapi;

import junit.framework.TestCase;

// Junit3 test frame created by Android Studio
public class SessionConnectorTest extends TestCase {

    public void testIsValidKulloAddress() {
        // hashes
        assertFalse(KulloUtils.isValidKulloAddress("testkullo.net"));
        assertTrue(KulloUtils.isValidKulloAddress("test#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("test#kullo#net"));

        // forbiddenCharsLocalPart
        assertFalse(KulloUtils.isValidKulloAddress("usr(name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr)name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr<name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr>name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr[name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr]name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr{name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr}name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr|name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr&name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr!name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr?name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr^name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr&name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr%name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr$name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr*name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr+name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr=name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr~name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr`name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr'name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr:name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr;name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr@name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr/name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr\\name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr,name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr\"name#kullo.net"));

        // localPartSeparators
        assertTrue(KulloUtils.isValidKulloAddress("user.name#kullo.net"));
        assertTrue(KulloUtils.isValidKulloAddress("user-name#kullo.net"));
        assertTrue(KulloUtils.isValidKulloAddress("user_name#kullo.net"));

        assertFalse(KulloUtils.isValidKulloAddress(".usr#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("-usr#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("_usr#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr.#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr-#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr_#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr..name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr--name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr__name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr.-name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr-.name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr-_name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr_-name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr_.name#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("usr._name#kullo.net"));

        // domain
        assertFalse(KulloUtils.isValidKulloAddress("test#.kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("test#kullo..net"));
        assertFalse(KulloUtils.isValidKulloAddress("test#kullo.net."));
        assertFalse(KulloUtils.isValidKulloAddress("test#kullo"));

        assertTrue(KulloUtils.isValidKulloAddress("test#ku-llo.net"));
        assertTrue(KulloUtils.isValidKulloAddress("test#kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("test#-kullo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("test#kullo-.net"));
        assertFalse(KulloUtils.isValidKulloAddress("test#ku--llo.net"));
        assertFalse(KulloUtils.isValidKulloAddress("test#kullo.-net"));
        assertFalse(KulloUtils.isValidKulloAddress("test#kullo.net-"));
        assertFalse(KulloUtils.isValidKulloAddress("test#kullo.123"));

        // chars
        assertTrue(KulloUtils.isValidKulloAddress("abcdefghijklmnopqrstuvwxyz#kullo.net"));
        assertTrue(KulloUtils.isValidKulloAddress("ABCDEFGHIJKLMNOPQRSTUVWXYZ#kullo.net"));
        assertTrue(KulloUtils.isValidKulloAddress("0123456789#kullo.net"));

        assertTrue(KulloUtils.isValidKulloAddress("test#abcdefghijklmnopqr.stuvwxyz"));
        assertTrue(KulloUtils.isValidKulloAddress("test#ABCDEFGHIJKLMNOPQR.STUVWXYZ"));
        assertTrue(KulloUtils.isValidKulloAddress("test#0123456789.com"));

        // length
        assertTrue(KulloUtils.isValidKulloAddress("a#kullo.net"));
        assertTrue(KulloUtils.isValidKulloAddress("a#b.c"));

        assertFalse(KulloUtils.isValidKulloAddress(""));
        assertFalse(KulloUtils.isValidKulloAddress("a#"));
        assertFalse(KulloUtils.isValidKulloAddress("#kullo.net"));

        String maxUsername = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"; // 64
        String maxDomainLabel = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"; // 63
        // max. 255 chars, here: 63 + 1 + 63 + 1 + 63 + 1 + 63 = 255
        String maxDomain = maxDomainLabel + "." + maxDomainLabel + "." + maxDomainLabel + "." + maxDomainLabel;
        assertTrue(KulloUtils.isValidKulloAddress(maxUsername + "#" + maxDomain));
        assertFalse(KulloUtils.isValidKulloAddress(maxUsername + "a#" + maxDomain));
        assertFalse(KulloUtils.isValidKulloAddress(maxUsername + "#b." + maxDomain));
        assertFalse(KulloUtils.isValidKulloAddress(maxUsername + "#b.b" + maxDomainLabel));
    }

    public void testIsValidMasterKeyBlock() {
        // wrong length
        assertFalse(KulloUtils.isValidMasterKeyBlock(""));
        assertFalse(KulloUtils.isValidMasterKeyBlock("0"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("00"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("000"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("0000"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("00000"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("0000000"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("00000000"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("000000000"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("0000000000"));

        // wrong chars (end, middle, begin)
        assertFalse(KulloUtils.isValidMasterKeyBlock("00000a"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("00000E"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("00000/"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("00000?"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("00000 "));
        assertFalse(KulloUtils.isValidMasterKeyBlock("00000\t"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("00000\n"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("000a00"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("000E00"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("000/00"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("000?00"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("000 00"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("000\t00"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("000\n00"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("a00"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("E00000"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("/00000"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("?00000"));
        assertFalse(KulloUtils.isValidMasterKeyBlock(" 00000"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("\t00000"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("\n00000"));

        // correct chars
        assertTrue(KulloUtils.isValidMasterKeyBlock("000000"));

        // Correct block modified at the end
        assertTrue(KulloUtils.isValidMasterKeyBlock("170530"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("170531"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("170532"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("170533"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("170534"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("170535"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("170536"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("170537"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("170538"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("170539"));

        // Correct block modified at the beginning
        assertTrue(KulloUtils.isValidMasterKeyBlock("170530"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("070530"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("270530"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("370530"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("470530"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("570530"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("670530"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("770530"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("870530"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("970530"));

        // Range (16 bit ~ [0, 65535])
        assertTrue(KulloUtils.isValidMasterKeyBlock("655357"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("655365"));
        assertFalse(KulloUtils.isValidMasterKeyBlock("999995"));
    }
}
