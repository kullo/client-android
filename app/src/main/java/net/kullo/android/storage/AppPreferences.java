/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.kullo.android.littlehelpers.KulloConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AppPreferences {

    final private SharedPreferences mSharedPref;

    public AppPreferences(@NonNull final Context context) {
        mSharedPref = context.getApplicationContext().getSharedPreferences(
            KulloConstants.PREFS_FILE_DEFAULT, Context.MODE_PRIVATE);
    }

    @Nullable
    public String get(@NonNull AppScopeKey key) {
        return mSharedPref.getString(getRawKey(key), null);
    }

    public void set(@NonNull AppScopeKey key, @NonNull String value) {
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putString(getRawKey(key), value);
        editor.apply();
    }

    public void clear(@NonNull AppScopeKey key) {
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.remove(getRawKey(key));
        editor.apply();
    }

    @Nullable
    public String get(@NonNull final String addressString, @NonNull UserScopeKey key) {
        String prefsKey = addressString + KulloConstants.SEPARATOR + getRawKey(key);
        return mSharedPref.getString(prefsKey, null);
    }

    public void set(@NonNull final String addressString, @NonNull UserScopeKey key, @NonNull String value) {
        String prefsKey = addressString + KulloConstants.SEPARATOR + getRawKey(key);
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putString(prefsKey, value);
        editor.apply();
    }

    @NonNull
    public ArrayList<String> getLoggedInAddresses() {
        final String suffix = KulloConstants.SEPARATOR + KulloConstants.BLOCK_A;
        ArrayList<String> storedAddresses = new ArrayList<>();
        for (String key : mSharedPref.getAll().keySet()) {
            if (key.endsWith(suffix)) {
                String address = key.substring(0, key.length() - suffix.length());
                storedAddresses.add(address);
            }
        }
        Collections.sort(storedAddresses);
        return storedAddresses;
    }

    @NonNull
    private String getRawKey(@NonNull AppScopeKey key) {
        switch (key) {
            case ACTIVE_USER:
                return KulloConstants.ACTIVE_USER;
            case LAST_ACTIVE_USER:
                return KulloConstants.LAST_ACTIVE_USER;
            case NEW_USER_NAME:
                return "new_registered_user_name";
            case NEW_USER_ORGANIZATION:
                return "new_registered_user_organization";
            default:
                throw new AssertionError("This must no happen");
        }
    }

    @NonNull
    private String getRawKey(@NonNull UserScopeKey key) {
        switch (key) {
            case MASTER_KEY_BLOCK_A: return KulloConstants.BLOCK_A;
            case MASTER_KEY_BLOCK_B: return KulloConstants.BLOCK_B;
            case MASTER_KEY_BLOCK_C: return KulloConstants.BLOCK_C;
            case MASTER_KEY_BLOCK_D: return KulloConstants.BLOCK_D;
            case MASTER_KEY_BLOCK_E: return KulloConstants.BLOCK_E;
            case MASTER_KEY_BLOCK_F: return KulloConstants.BLOCK_F;
            case MASTER_KEY_BLOCK_G: return KulloConstants.BLOCK_G;
            case MASTER_KEY_BLOCK_H: return KulloConstants.BLOCK_H;
            case MASTER_KEY_BLOCK_I: return KulloConstants.BLOCK_I;
            case MASTER_KEY_BLOCK_J: return KulloConstants.BLOCK_J;
            case MASTER_KEY_BLOCK_K: return KulloConstants.BLOCK_K;
            case MASTER_KEY_BLOCK_L: return KulloConstants.BLOCK_L;
            case MASTER_KEY_BLOCK_M: return KulloConstants.BLOCK_M;
            case MASTER_KEY_BLOCK_N: return KulloConstants.BLOCK_N;
            case MASTER_KEY_BLOCK_O: return KulloConstants.BLOCK_O;
            case MASTER_KEY_BLOCK_P: return KulloConstants.BLOCK_P;
            default:
                throw new AssertionError("This must no happen");
        }
    }

    public enum AppScopeKey {
        ACTIVE_USER,
        LAST_ACTIVE_USER,
        NEW_USER_NAME,
        NEW_USER_ORGANIZATION,
    }

    public enum UserScopeKey {
        MASTER_KEY_BLOCK_A,
        MASTER_KEY_BLOCK_B,
        MASTER_KEY_BLOCK_C,
        MASTER_KEY_BLOCK_D,
        MASTER_KEY_BLOCK_E,
        MASTER_KEY_BLOCK_F,
        MASTER_KEY_BLOCK_G,
        MASTER_KEY_BLOCK_H,
        MASTER_KEY_BLOCK_I,
        MASTER_KEY_BLOCK_J,
        MASTER_KEY_BLOCK_K,
        MASTER_KEY_BLOCK_L,
        MASTER_KEY_BLOCK_M,
        MASTER_KEY_BLOCK_N,
        MASTER_KEY_BLOCK_O,
        MASTER_KEY_BLOCK_P,
    }

    public static final List<UserScopeKey> MASTER_KEY_KEYS = Arrays.asList(
        UserScopeKey.MASTER_KEY_BLOCK_A,
        UserScopeKey.MASTER_KEY_BLOCK_B,
        UserScopeKey.MASTER_KEY_BLOCK_C,
        UserScopeKey.MASTER_KEY_BLOCK_D,
        UserScopeKey.MASTER_KEY_BLOCK_E,
        UserScopeKey.MASTER_KEY_BLOCK_F,
        UserScopeKey.MASTER_KEY_BLOCK_G,
        UserScopeKey.MASTER_KEY_BLOCK_H,
        UserScopeKey.MASTER_KEY_BLOCK_I,
        UserScopeKey.MASTER_KEY_BLOCK_J,
        UserScopeKey.MASTER_KEY_BLOCK_K,
        UserScopeKey.MASTER_KEY_BLOCK_L,
        UserScopeKey.MASTER_KEY_BLOCK_M,
        UserScopeKey.MASTER_KEY_BLOCK_N,
        UserScopeKey.MASTER_KEY_BLOCK_O,
        UserScopeKey.MASTER_KEY_BLOCK_P
    );

}
