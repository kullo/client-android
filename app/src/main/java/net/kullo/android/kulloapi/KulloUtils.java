/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
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

import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class KulloUtils {
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[_\\W]");

    public static String generateInitialsForAddressAndName(@Nullable String name) {
        List<String> out = new LinkedList<>();

        if (name != null && !name.isEmpty()) {
            String[] nameParts = WHITESPACE.split(name);

            for (String namePart : nameParts) {
                namePart = NON_ALPHANUMERIC.matcher(namePart).replaceAll("");

                if (!namePart.isEmpty()) {
                    out.add(getFirstUnicodeCharacter(namePart));

                    if (out.size() == 2) break;
                }
            }
        }

        return StringUtils.join(out, "");
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
        return progress.getCountTotal() > 3 &&
                (100.0 * progress.getCountProcessed() / progress.getCountTotal()) >= 5.0;
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
