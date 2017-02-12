/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.kulloapi;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.Address;
import net.kullo.libkullo.api.DateTime;
import net.kullo.libkullo.api.MasterKey;
import net.kullo.libkullo.api.SyncProgress;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KulloUtils {
    // match all words, beginning with a letter or a digit (alnum + (word|-)*)
    private static final Pattern WORD = Pattern.compile("\\p{Alnum}(?:-|\\w)*");

    public static String generateInitialsForAddressAndName(@Nullable String name) {
        String out = "";

        if (name != null && !name.isEmpty()) {
            Matcher matcher = WORD.matcher(name.toUpperCase());
            List<String> nameParts = new ArrayList<>();

            while (matcher.find()) {
                nameParts.add(matcher.group());
            }

            if (nameParts.size() >= 1) {
                out += getFirstUnicodeCharacter(nameParts.get(0));
            }
            if (nameParts.size() >= 2) {
                out += getFirstUnicodeCharacter(nameParts.get(nameParts.size() - 1));
            }
        }

        return out;
    }

    @NonNull
    private static String getFirstUnicodeCharacter(String namePart) {
        if (namePart.length() >= 2) {
            if (Character.isSurrogatePair(namePart.charAt(0), namePart.charAt(1)))
            {
                return namePart.substring(0, 2);
            }
        }

        return namePart.substring(0, 1);
    }

    public static boolean showSyncProgressAsBar(final SyncProgress progress) {
        return progress.getIncomingMessagesTotal() > 3 &&
                (100.0 * progress.getIncomingMessagesProcessed() / progress.getIncomingMessagesTotal()) >= 5.0;
    }

    public static boolean isValidKulloAddress(String address) {
        return (Address.create(address) != null);
    }

    public static boolean isValidMasterKeyBlock(String block) {
        return MasterKey.isValidBlock(block);
    }

    @NonNull
    public static org.joda.time.DateTime convertToJodaTime(DateTime kulloDateTime) {
        RuntimeAssertion.require(kulloDateTime != null);
        return org.joda.time.DateTime.parse(kulloDateTime.toString());
    }

    @NonNull
    static String getDatabasePathBase(Application app, Address address) {
        Context appContext = app.getApplicationContext();
        String appDir = appContext.getFilesDir().getAbsolutePath();

        return appDir + "/" + address.toString() + ".db";
    }
}
