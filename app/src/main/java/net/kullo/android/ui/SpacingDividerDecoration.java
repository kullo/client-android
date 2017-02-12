/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.ui;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class SpacingDividerDecoration extends RecyclerView.ItemDecoration {
    private final int mPixels;

    public SpacingDividerDecoration(Context context, int dimenResourceId) {
        mPixels = context.getResources().getDimensionPixelSize(dimenResourceId);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view,
                               RecyclerView parent,
                               RecyclerView.State state) {
        outRect.top = mPixels;

        // Add spacing below last item
        int pos = parent.getChildAdapterPosition(view);
        if (pos == state.getItemCount() - 1) {
            outRect.bottom = mPixels;
        }
    }
}
