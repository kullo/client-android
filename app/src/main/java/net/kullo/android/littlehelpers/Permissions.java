/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import net.kullo.android.application.KulloApplication;

public class Permissions {
    public static boolean checkOrRequestReadPermission(@NonNull final Activity activity) {
        // permission is always granted on pre Android 6 devices
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;

        final String requestedPermission = Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(activity, requestedPermission) != PackageManager.PERMISSION_GRANTED) {
            // No explanation for the user is needed in this case
            ActivityCompat.requestPermissions(activity, new String[]{requestedPermission},
                    KulloApplication.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            return false;
        } else {
            return true;
        }
    }
}