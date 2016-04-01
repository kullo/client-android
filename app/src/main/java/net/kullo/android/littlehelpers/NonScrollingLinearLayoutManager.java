/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;

public class NonScrollingLinearLayoutManager extends LinearLayoutManager {
    public NonScrollingLinearLayoutManager(Context context) {
        super(context);
    }

    public NonScrollingLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    @Override
    public boolean canScrollVertically() {
        return false;
    }

    @Override
    public boolean canScrollHorizontally() {
        return false;
    }
}
