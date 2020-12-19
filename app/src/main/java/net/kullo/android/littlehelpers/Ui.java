/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.littlehelpers;

import android.app.Activity;
import android.app.ActivityManager.TaskDescription;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import net.kullo.android.R;
import net.kullo.javautils.RuntimeAssertion;

public class Ui {
    public enum LayoutType {
        DrawerLayout,
        CoordinatorLayout,
        Other
    }

    public static void setStatusBarColor(Activity activity) {
        setStatusBarColor(activity, false, LayoutType.Other);
    }

    public static void setStatusBarColor(Activity activity, boolean actionModeEnabled) {
        setStatusBarColor(activity, actionModeEnabled, LayoutType.Other);
    }

    public static void setStatusBarColor(Activity activity, boolean actionModeEnabled, LayoutType type) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            /*
             * By default, CoordinatorLayout and DrawerLayout draw colorPrimaryDark
             * below statusBarColor.
             * Other layouts draw windowBackground below statusBarColor.
             *
             * CoordinatorLayout/DrawerLayout must use colorPrimary as background plus a system defined
             * dark shadow.
             * Others must use a pre-calculated statusBarColor = colorPrimary + dark shadow
             *
             */
            @ColorInt int color;
            switch (type) {
                case DrawerLayout:
                case CoordinatorLayout:
                    // Use system default semi transparent black
                    color = 0x33000000; // 20 % black
                    break;
                case Other:
                    color = 0xffBA7102; // primary orange + 20 % black, precalculated
                    break;
                default:
                    color = 0;
                    RuntimeAssertion.fail();
            }
            activity.getWindow().setStatusBarColor(color);
        }
    }

    public static void setStatusBarColor(DrawerLayout layout) {
        int kulloPrimary = layout.getResources().getColor(R.color.kulloPrimaryColor);
        layout.setStatusBarBackgroundColor(kulloPrimary);
    }

    public static void setStatusBarColor(CoordinatorLayout layout) {
        int kulloPrimary = layout.getResources().getColor(R.color.kulloPrimaryColor);
        layout.setStatusBarBackgroundColor(kulloPrimary);
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

    public static void prepareActivityForTaskManager(@NonNull AppCompatActivity activity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            TaskDescription td = new TaskDescription(
                    activity.getResources().getString(R.string.app_name),
                    BitmapFactory.decodeResource(activity.getResources(), R.drawable.kullo_app_switcher),
                    activity.getResources().getColor(R.color.kulloPrimaryDarkColor));
            activity.setTaskDescription(td);
        }
    }
}
