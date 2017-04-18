/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.startconversation;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.kullo.android.R;
import net.kullo.android.littlehelpers.AddressSet;
import net.kullo.android.util.adapters.AddressesAdapter;
import net.kullo.libkullo.api.Address;

public class ParticipantsAdapter extends AddressesAdapter<ParticipantsViewHolder> {

    @NonNull private final Context mContext;

    public interface OnClickListener {
        void onClick(View v, int position);
    }
    public interface OnLongClickListener {
        boolean onLongClick(View v, int position);
    }

    @Nullable private OnLongClickListener mOnLongClickListener;
    @Nullable private OnClickListener mOnClickListener;

    public ParticipantsAdapter(@NonNull Context context) {
        super(new AddressSet());
        mContext = context;
    }

    @Override
    public ParticipantsViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.row_participant, viewGroup, false);

        return new ParticipantsViewHolder(itemView);
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        mOnClickListener = onClickListener;
    }

    public void setOnLongClickListener(OnLongClickListener onLongClickListener) {
        mOnLongClickListener = onLongClickListener;
    }

    @Override
    public void onBindViewHolder(final ParticipantsViewHolder viewHolder, int i) {
        Address participant = getItem(i);
        viewHolder.mParticipantAddress.setText(participant.toString());
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = viewHolder.getAdapterPosition();
                if (mOnClickListener != null) mOnClickListener.onClick(v, pos);
            }
        });
        viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @SuppressWarnings("SimplifiableIfStatement")
            @Override
            public boolean onLongClick(View v) {
                int pos = viewHolder.getAdapterPosition();
                if (mOnLongClickListener != null) return mOnLongClickListener.onLongClick(v, pos);
                else return false;
            }
        });

        if (isSelected(participant)) {
            viewHolder.itemView.setBackgroundColor(mContext.getResources().getColor(R.color.kulloSelectionColor));
        } else {
            viewHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }
}
