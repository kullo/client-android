/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.ui;

import android.content.Context;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

public class ImageView16by9 extends AppCompatImageView {

    public ImageView16by9(Context context) {
        super(context);
    }

    public ImageView16by9(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImageView16by9(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO: take minimum height / width into account
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = Math.round((width/16.0f) * 9.0f);
        setMeasuredDimension(width, height);
    }
}
