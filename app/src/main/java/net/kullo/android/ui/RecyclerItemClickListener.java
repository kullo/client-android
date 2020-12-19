/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

abstract public class RecyclerItemClickListener implements RecyclerView.OnItemTouchListener {

    // Implement those functions when using RecyclerItemClickListener
    abstract public void onItemClick(View view, int position);
    abstract public void onItemLongPress(View view, int position);

    private GestureDetector mGestureDetector;

    public RecyclerItemClickListener(final RecyclerView view) {
        Context context = view.getContext();
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                View childView = view.findChildViewUnder(e.getX(), e.getY());
                if (childView != null) {
                    int position = view.getChildAdapterPosition(childView);
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClick(childView, position);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                View childView = view.findChildViewUnder(e.getX(), e.getY());
                if (childView != null) {
                    int position = view.getChildAdapterPosition(childView);
                    if (position != RecyclerView.NO_POSITION) {
                        onItemLongPress(childView, position);
                    }
                }
            }
        });
    }

    // Always return false to silently observe events without stopping scrolls
    // from working
    @Override
    final public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent e) {
        mGestureDetector.onTouchEvent(e);
        return false;
    }

    // This is never called since onInterceptTouchEvent never returns true
    @Override
    final public void onTouchEvent(RecyclerView view, MotionEvent e) {
    }

    // Not supported
    @Override
    final public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }
}
