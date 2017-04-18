/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import net.kullo.javautils.RuntimeAssertion;

public class DividerDecoration extends RecyclerView.ItemDecoration {
    public final static String TAG = "DividerDecoration";

    private static final int[] ATTRS = new int[]{
            android.R.attr.listDivider
    };

    private Drawable mDividerDrawable;
    private int mDividerIntrinsicHeight;
    private int mLeftMargin;

    public DividerDecoration(Context context, int leftMargin) {
        mLeftMargin = leftMargin;

        final TypedArray a = context.obtainStyledAttributes(ATTRS);
        mDividerDrawable = a.getDrawable(0);
        RuntimeAssertion.require(mDividerDrawable != null);

        mDividerIntrinsicHeight = mDividerDrawable.getIntrinsicHeight();
        if (mDividerIntrinsicHeight == -1) {
            throw new RuntimeException("Divider has no intrinsic height. "
                    + "This happens if a color is set instead of a shape drawable");
        }
        a.recycle();
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        super.onDraw(c, parent, state);

        final int left = parent.getPaddingLeft() + mLeftMargin;
        final int right = parent.getWidth() - parent.getPaddingRight();
        final int recyclerViewTop = parent.getPaddingTop();
        final int recyclerViewBottom = parent.getHeight() - parent.getPaddingBottom();
        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            if(isDecorated(child, parent)) {
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                    .getLayoutParams();
                final int top = Math.max(recyclerViewTop, child.getBottom() + params.bottomMargin);
                final int bottom = Math.min(recyclerViewBottom, top + mDividerIntrinsicHeight);
                mDividerDrawable.setBounds(left, top, right, bottom);
                mDividerDrawable.draw(c);
            }
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        if (isDecorated(view, parent)) {
            outRect.set(0, 0, 0, mDividerIntrinsicHeight);
        }
    }

    protected boolean isDecorated(View view, RecyclerView parent) {
        return true;
    }
}
