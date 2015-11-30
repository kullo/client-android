/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.kulloapi;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.Address;
import net.kullo.libkullo.api.DateTime;
import net.kullo.libkullo.api.MasterKey;

public class KulloUtils {
    public static String generateInitialsForAddressAndName(@NonNull Address address, @Nullable String name) {
        String out = "";

        if (name != null && !name.isEmpty()) {
            String[] nameParts = name.split(" ");
            if (nameParts.length == 1) {
                out = name.substring(0, 1);
            } else if (nameParts.length >= 2) {
                out = nameParts[0].substring(0, 1) + nameParts[nameParts.length - 1].substring(0, 1);
            }
        } else {
            String addressLocalPart = address.localPart();
            out = addressLocalPart.length() <= 2
                    ? addressLocalPart
                    : addressLocalPart.substring(0, 2);
        }

        return out.toUpperCase();
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
