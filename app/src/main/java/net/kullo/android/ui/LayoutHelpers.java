/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.ui;

import android.support.annotation.NonNull;
import android.support.annotation.Px;
import android.view.View;
import android.view.ViewGroup;

public class LayoutHelpers {

    public static void setLeftMargin(@NonNull View target, @Px int value) {
        ViewGroup.LayoutParams params = target.getLayoutParams();
        ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) params;
        if (marginParams != null) {
            marginParams.setMargins(value, marginParams.topMargin, marginParams.rightMargin, marginParams.bottomMargin);
            target.setLayoutParams(params);
        }
    }

    public static void setRightMargin(@NonNull View target, @Px int value) {
        ViewGroup.LayoutParams params = target.getLayoutParams();
        ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) params;
        if (marginParams != null) {
            marginParams.setMargins(marginParams.leftMargin, marginParams.topMargin, value, marginParams.bottomMargin);
            target.setLayoutParams(params);
        }
    }
}
