/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.application;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import net.danlew.android.joda.JodaTimeAndroid;
import net.kullo.android.kulloapi.ClientConnector;
import net.kullo.android.kulloapi.KulloUtils;
import net.kullo.android.littlehelpers.CiStringComparator;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.LibKullo;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class KulloApplication extends Application
        implements Application.ActivityLifecycleCallbacks {
    public static final String TAG = "KulloApplication";
    public static final Uri MAINTAINER_WEBSITE = Uri.parse("https://www.kullo.net");
    public static final String LICENSES_FILE = "file:///android_asset/licenses-android.html";
    public static final String ID = "net.kullo.android";

    public static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1001;

    private static final int LATEST_PREFERENCES_VERSION = 3;

    // only access this variable in updateForegroundActivitiesCount()
    private int mCountActivitiesInForeground = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);

        JodaTimeAndroid.init(this);
        LibKullo.init();

        deleteObsoletePreferences();
        migratePreferences(getSharedPreferences(KulloConstants.ACCOUNT_PREFS_PLAIN, Context.MODE_PRIVATE));

        cleanExternalFilesDir();
    }

    private void cleanExternalFilesDir() {
        // Cleans the entire app-specific external files dir. This was used before
        // version 35 (released 2016-08-03) as temporary storage when handling attachments.
        // We currently do not use this directory. Database is in internal files (getFilesDir())
        // and temporary files are in the app's internal cache directory (getCacheDir()).
        final File appExternal = getExternalFilesDir(null);
        if (appExternal != null) {
            Log.d(TAG, "Cleaning " + appExternal.getAbsolutePath() + " ...");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    cleanDirectory(appExternal);
                }
            }).run();
        } else {
            Log.w(TAG, "Shared storage is currently not available.");
        }
    }

    private void cleanDirectory(@NonNull File path) {
        for (File entry : path.listFiles()) {
            if (entry.isDirectory()) {
                cleanDirectory(entry);

                // directory should be empty now
                if (entry.delete()) {
                    Log.d(TAG, "Successfully removed directory " + entry.getAbsolutePath());
                } else {
                    Log.e(TAG, "Error deleting directory " + entry.getAbsolutePath());
                }
            } else if (entry.isFile()) {
                if (entry.delete()) {
                    Log.d(TAG, "Successfully removed " + entry.getAbsolutePath());
                } else {
                    Log.e(TAG, "Error deleting " + entry.getAbsolutePath());
                }
            }
        }
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

    @NonNull
    public File cacheDir(final CacheType type, @Nullable String customSubfolderName) {
        final String typeSubfolderName;
        switch (type) {
            case AddAttachment:
                typeSubfolderName = "add_attachment"; break;
            case Capture:
                typeSubfolderName = "capture"; break;
            case OpenFile:
                typeSubfolderName = "openfile"; break;
            case ReceivedShares:
                typeSubfolderName = "received_shares"; break;
            default:
                typeSubfolderName = "";
                RuntimeAssertion.fail("Unhandled cache type");
        }
        return cacheDirWithSubfolder(typeSubfolderName, customSubfolderName);
    }

    @NonNull
    private File cacheDirWithSubfolder(@NonNull final String subfolderName,
                                       @Nullable final String subfolder2Name) {
        final File fileOpenCacheDir = new File(getCacheDir(), subfolderName);
        ensureDirExists(fileOpenCacheDir);

        if (subfolder2Name == null) {
            return fileOpenCacheDir;
        } else {
            final File dir = new File(fileOpenCacheDir, subfolder2Name);
            ensureDirExists(dir);
            return dir;
        }
    }

    private void ensureDirExists(@NonNull final File dir) {
        if (!dir.isDirectory()) {
            boolean created = dir.mkdir();
            if (!created) throw new RuntimeException("Did not create cache directory");
        }
    }

    public boolean canOpenFileType(File file, String mimeType) {
        Intent openFileIntent = new Intent();
        openFileIntent.setAction(android.content.Intent.ACTION_VIEW);
        openFileIntent.setDataAndType(Uri.fromFile(file), mimeType);

        final PackageManager packageManager = getPackageManager();
        List results = packageManager.queryIntentActivities(openFileIntent, 0);

        return !results.isEmpty();
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

    private void deleteObsoletePreferences() {
        // delete encrypted prefs
        SharedPreferences old1 = getSharedPreferences(KulloConstants.ACCOUNT_PREFS_OBSOLETE, Context.MODE_PRIVATE);
        old1.edit().clear().apply();

        // remove former obsolete preferences
        SharedPreferences old2 = getSharedPreferences(KulloConstants.USER_SETTINGS_PREFS_OBSOLETE, Context.MODE_PRIVATE);
        old2.edit().clear().apply();
    }

    // Do not increase LATEST_PREFERENCES_VERSION for tests because it will
    // migrate production data since AndroidTestCase creates an Application.
    private static void migratePreferences(SharedPreferences currentPrefs) {
        migratePreferences(currentPrefs, LATEST_PREFERENCES_VERSION);
    }

    public static void migratePreferences(SharedPreferences currentPrefs, int versionLimit) {
        final int currentPreferencesVersion = currentPrefs.getInt(KulloConstants.ACCOUNT_PREFS_VERSION_KEY, 0);

        if (currentPreferencesVersion < versionLimit) {
            Log.i(TAG, "Preferences migration necessary. Current: " + currentPreferencesVersion
                    + " Latest: " + versionLimit);

            int version = currentPreferencesVersion;

            if (version == 0 && version < versionLimit) {
                // Before this migration, only one account was stored. So we can
                // assume that there 0 or 1 accounts.
                version++;
                Log.i(TAG, "Migrating preferences to version " + version + " ...");
                SharedPreferences.Editor editor = currentPrefs.edit();

                String addressString = currentPrefs.getString(KulloConstants.KULLO_ADDRESS, "");

                if (!addressString.isEmpty()) {
                    // move address
                    editor.putString(KulloConstants.ACTIVE_USER, addressString);
                    editor.putString(KulloConstants.LAST_ACTIVE_USER, addressString);
                    editor.remove(KulloConstants.KULLO_ADDRESS);

                    // move master key
                    for (String oldKey : KulloConstants.BLOCK_KEYS_AS_LIST) {
                        String newKey = addressString + "_" + oldKey;
                        movePreferencesStringEntry(currentPrefs, editor, oldKey, newKey);
                    }
                }

                editor.putInt(KulloConstants.ACCOUNT_PREFS_VERSION_KEY, version);
                editor.apply();
            }

            if (version == 1 && version < versionLimit)
            {
                // Before this migration, multiple accounts can be stored
                version++;
                Log.i(TAG, "Migrating preferences to version " + version + " ...");
                SharedPreferences.Editor editor = currentPrefs.edit();

                final String oldSeparator = "_";
                final String newSeparator = "|";

                final List<String> fieldsOrdered = Arrays.asList(
                        "user_avatar_mime_type", // must be before user_avatar to ensure "mime_type" is not matched as part of the Kullo address
                        "user_avatar",
                        "user_organization",
                        "user_name",
                        "user_footer"
                );

                // key_address -> key|address
                for (String oldKey : currentPrefs.getAll().keySet()) {
                    boolean currentKeyMoved = false;
                    for (String field : fieldsOrdered) {
                        if (!currentKeyMoved && oldKey.startsWith(field + oldSeparator)) {
                            String address = oldKey.substring(field.length() + oldSeparator.length());
                            if (KulloUtils.isValidKulloAddress(address)) {
                                String newKey = field + newSeparator + address;
                                Log.d(TAG, "Moving preferences entry from " + oldKey + " to " + newKey);
                                movePreferencesStringEntry(currentPrefs, editor, oldKey, newKey);
                                currentKeyMoved = true;
                            }
                        }
                    }
                }

                // address_key -> address|key
                for (String oldKey : currentPrefs.getAll().keySet()) {
                    boolean currentKeyMoved = false;
                    for (String field : KulloConstants.BLOCK_KEYS_AS_LIST) {
                        if (!currentKeyMoved && oldKey.endsWith(oldSeparator + field)) {
                            String address = oldKey.substring(0,
                                    oldKey.length() - field.length() - oldSeparator.length());
                            if (KulloUtils.isValidKulloAddress(address)) {
                                String newKey = address + newSeparator + field;
                                Log.d(TAG, "Moving preferences entry from " + oldKey + " to " + newKey);
                                movePreferencesStringEntry(currentPrefs, editor, oldKey, newKey);
                                currentKeyMoved = true;
                            }
                        }
                    }
                }

                editor.putInt(KulloConstants.ACCOUNT_PREFS_VERSION_KEY, version);
                editor.apply();
            }

            if (version == 2 && version < versionLimit) {
                // Before this migration, multiple accounts can be stored
                version++;
                Log.i(TAG, "Migrating preferences to version " + version + " ...");
                SharedPreferences.Editor editor = currentPrefs.edit();

                final List<String> fieldsOrdered = Arrays.asList(
                        "user_avatar_mime_type", // must be before user_avatar to ensure "mime_type" is not matched as part of the Kullo address
                        "user_avatar",
                        "user_organization",
                        "user_name",
                        "user_footer"
                );

                final String separator = "|";

                // key|address -> address|key
                for (String oldKey : currentPrefs.getAll().keySet()) {
                    boolean currentKeyMoved = false;
                    for (String field : fieldsOrdered) {
                        if (!currentKeyMoved && oldKey.startsWith(field + separator)) {
                            String address = oldKey.substring(field.length() + separator.length());
                            if (KulloUtils.isValidKulloAddress(address)) {
                                String newKey = address + separator + field;
                                Log.d(TAG, "Moving preferences entry from " + oldKey + " to " + newKey);
                                movePreferencesStringEntry(currentPrefs, editor, oldKey, newKey);
                                currentKeyMoved = true;
                            }
                        }
                    }
                }

                editor.putInt(KulloConstants.ACCOUNT_PREFS_VERSION_KEY, version);
                editor.apply();
            }

        } else {
            Log.d(TAG, "No preferences migration necessary.");
        }
    }

    private static void movePreferencesStringEntry(SharedPreferences sharedPreferences,
                                                   SharedPreferences.Editor editor,
                                                   String oldKey, String newKey) {
        String value = sharedPreferences.getString(oldKey, "");
        editor.putString(newKey, value);
        editor.remove(oldKey);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        int newValue = updateForegroundActivitiesCount(+1);
        Log.d(TAG, "Foreground count: " + newValue);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        int newValue = updateForegroundActivitiesCount(-1);
        Log.d(TAG, "Foreground count: " + newValue);
    }

    public int foregroundActivitiesCount() {
        return updateForegroundActivitiesCount(0);
    }

    // returns the new value
    synchronized private int updateForegroundActivitiesCount(int change) {
        mCountActivitiesInForeground += change;
        return mCountActivitiesInForeground;
    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
