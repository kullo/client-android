/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

import android.app.Activity;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import net.kullo.android.R;
import net.kullo.javautils.RuntimeAssertion;

public class Ui {
    public static void setColorStatusBarArrangeHeader(Activity activity) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().setStatusBarColor(activity.getResources().getColor(R.color.kulloPrimaryDarkColor));
        }
    }

    @NonNull
    public static Toolbar setupActionbar(@NonNull AppCompatActivity activity) {
        return setupActionbar(activity, true);
    }

    @NonNull
    public static Toolbar setupActionbar(@NonNull AppCompatActivity activity, boolean homeAsUp) {
        final Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar);
        RuntimeAssertion.require(toolbar != null);
        activity.setSupportActionBar(toolbar);

        {
            ActionBar supportActionBar = activity.getSupportActionBar();
            RuntimeAssertion.require(supportActionBar != null);
            supportActionBar.setDisplayHomeAsUpEnabled(homeAsUp);
        }

        return toolbar;
    }
}
