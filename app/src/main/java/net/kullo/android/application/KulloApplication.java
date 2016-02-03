/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.application;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;

import net.danlew.android.joda.JodaTimeAndroid;
import net.kullo.android.kulloapi.ClientConnector;
import net.kullo.android.littlehelpers.CiStringComparator;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.libkullo.LibKullo;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class KulloApplication extends Application {
    public static final Uri MAINTAINER_WEBSITE = Uri.parse("https://www.kullo.net");
    public static final String LICENSES_FILE = "file:///android_asset/licenses-android.html";

    @Override
    public void onCreate() {
        super.onCreate();

        JodaTimeAndroid.init(this);
        LibKullo.init();

        migratePreferences();
    }

    public DateTimeFormatter getShortDateFormatter() {
        DateFormat systemDateFormat = android.text.format.DateFormat.getDateFormat(this);
        String pattern = ((SimpleDateFormat) systemDateFormat). toLocalizedPattern();
        return DateTimeFormat.forPattern(pattern);
    }

    public DateTimeFormatter getShortTimeFormatter() {
        DateFormat systemDateFormat = android.text.format.DateFormat.getTimeFormat(this);
        String pattern = ((SimpleDateFormat) systemDateFormat). toLocalizedPattern();
        return DateTimeFormat.forPattern(pattern);
    }

    public DateTimeFormatter getFullDateTimeFormatter() {
        DateFormat systemDateFormat = android.text.format.DateFormat.getMediumDateFormat(this);
        DateFormat systemTimeFormat = android.text.format.DateFormat.getTimeFormat(this);
        String pattern = ((SimpleDateFormat) systemDateFormat).toLocalizedPattern() + ", " +
            ((SimpleDateFormat) systemTimeFormat).toLocalizedPattern();
        return DateTimeFormat.forPattern(pattern);
    }

    public boolean canOpenFileType(File file, String mimeType) {
        Intent openFileIntent = new Intent();
        openFileIntent.setAction(android.content.Intent.ACTION_VIEW);
        openFileIntent.setDataAndType(Uri.fromFile(file), mimeType);

        final PackageManager packageManager = getPackageManager();
        List results = packageManager.queryIntentActivities(openFileIntent, 0);

        if (results.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    public String softwareVersions() {
        StringBuilder out = new StringBuilder();
        HashMap<String, String> table = ClientConnector.get().getClient().versions();
        List<String> keys = new ArrayList<>(table.keySet());
        Collections.sort(keys, new CiStringComparator());
        for (String key : keys) {
            if (out.length() != 0) out.append(", ");
            out.append(key);
            out.append(" ");
            out.append(table.get(key));
        }
        return out.toString();
    }

    private void migratePreferences() {
        // delete encrypted prefs
        SharedPreferences old1 = getSharedPreferences(KulloConstants.ACCOUNT_PREFS_OBSOLETE, Context.MODE_PRIVATE);
        old1.edit().clear().apply();

        // remove former obsolete preferences
        SharedPreferences old2 = getSharedPreferences(KulloConstants.USER_SETTINGS_PREFS_OBSOLETE, Context.MODE_PRIVATE);
        old2.edit().clear().apply();
    }
}
