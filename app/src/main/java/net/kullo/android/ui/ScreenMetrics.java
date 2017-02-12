/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.ui;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;

import net.kullo.javautils.RuntimeAssertion;

public class ScreenMetrics {

    public static float px2dp(Context context, final int pixels) {
        final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        final float density = displayMetrics.density;
        return pixels / density;
    }

    public static int getColumnsForComponent(final View component, final float minColumnsWidthDp) {
        RuntimeAssertion.require(minColumnsWidthDp > 0);

        float widthDp = px2dp(component.getContext(), component.getWidth());
        //Log.d(TAG, "width: " + widthDp + "dp");
        int columns = (int) Math.floor(widthDp / minColumnsWidthDp);
        if (columns < 1) columns = 1;
        return columns;
    }
}
