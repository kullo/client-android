/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class AppVersion {
    private Application mApp;

    public AppVersion(Application app) {
        mApp = app;
    }

    public String versionName() {
        String versionName = null;
        final Context context = mApp.getApplicationContext();

        final PackageManager packageManager = context.getPackageManager();
        if (packageManager != null) {
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
                versionName = packageInfo.versionName;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }

        return versionName;
    }
}
