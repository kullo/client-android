/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
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
