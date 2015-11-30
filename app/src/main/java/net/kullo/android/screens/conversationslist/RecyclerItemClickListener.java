/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.conversationslist;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class RecyclerItemClickListener implements RecyclerView.OnItemTouchListener {
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
        void onItemLongPress(View view, int position);
    }

    GestureDetector mGestureDetector;

    public RecyclerItemClickListener(Context context, final RecyclerView view, OnItemClickListener listener) {
        mListener = listener;
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                View childView = view.findChildViewUnder(e.getX(), e.getY());
                if (childView != null && mListener != null) {
                    int position = view.getChildAdapterPosition(childView);
                    if (position != RecyclerView.NO_POSITION) {
                        mListener.onItemClick(childView, position);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                View childView = view.findChildViewUnder(e.getX(), e.getY());
                if (childView != null && mListener != null) {
                    int position = view.getChildAdapterPosition(childView);
                    if (position != RecyclerView.NO_POSITION) {
                        mListener.onItemLongPress(childView, position);
                    }
                }
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent e) {
        return mGestureDetector.onTouchEvent(e);
    }

    // called when dragging (touch moved events)
    @Override
    public void onTouchEvent(RecyclerView view, MotionEvent motionEvent) { }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        // do nothing
    }
}
