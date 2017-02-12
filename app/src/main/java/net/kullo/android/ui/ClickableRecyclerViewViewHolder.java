/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.ui;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class ClickableRecyclerViewViewHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener, View.OnLongClickListener {

    @Nullable private OnItemClickListener mOnItemClickListener = null;
    @Nullable private OnItemLongClickListener mOnItemLongClickListener = null;

    public interface OnItemClickListener {
        public void onItemClicked(int adapterPosition);
    }

    public interface OnItemLongClickListener {
        public boolean onItemLongClicked(int adapterPosition);
    }

    public ClickableRecyclerViewViewHolder(View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);
        itemView.setOnLongClickListener(this);
    }

    public void bindOnClickListener(final OnItemClickListener l) {
        mOnItemClickListener = l;
    }

    public void bindOnItemLongClickListener(final OnItemLongClickListener l) {
        mOnItemLongClickListener = l;
    }

    @Override
    public void onClick(View view) {
        if (mOnItemClickListener != null) {
            mOnItemClickListener.onItemClicked(getAdapterPosition());
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (mOnItemLongClickListener != null) {
            return mOnItemLongClickListener.onItemLongClicked(getAdapterPosition());
        }
        return false;
    }
}
