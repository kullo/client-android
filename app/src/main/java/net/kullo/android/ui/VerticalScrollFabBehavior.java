/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.ui;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

public class VerticalScrollFabBehavior extends FloatingActionButton.Behavior {

    public VerticalScrollFabBehavior(Context context, AttributeSet attrs) {
        super();
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, FloatingActionButton child, View dependency) {
        if (dependency instanceof RecyclerView) {
            return true;
        } else {
            return super.layoutDependsOn(parent, child, dependency);
        }
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, FloatingActionButton child, View directTargetChild, View target, int nestedScrollAxes) {
        if (nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL) {
            return true;
        } else {
            return super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, nestedScrollAxes);
        }
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, FloatingActionButton fab, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        super.onNestedScroll(coordinatorLayout, fab, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);

        if (dyConsumed > 0 && fab.getVisibility() == View.VISIBLE) {
            // scrolling down
            fab.hide(new FloatingActionButton.OnVisibilityChangedListener() {
                @Override
                public void onHidden(FloatingActionButton fab) {
                    super.onHidden(fab);

                    // Hide animation ended. FloatingActionButton implementation did set
                    // visibility to GONE, which must be fixed. See
                    // http://stackoverflow.com/questions/41142711/25-1-0-android-support-lib-is-breaking-fab-behavior
                    fab.setVisibility(View.INVISIBLE);
                }
            });
        } else if (dyConsumed < 0 && fab.getVisibility() != View.VISIBLE) {
            // scrolling up
            fab.show();
        }
    }
}
