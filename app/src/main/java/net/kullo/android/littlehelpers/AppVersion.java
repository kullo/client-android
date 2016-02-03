/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * Created by simon on 11.09.15.
 */
public class AppVersion {
    Application mApp;

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
            } catch (PackageManager.NameNotFoundException e) {
            }
        }

        return versionName;
    }
}
