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
import net.kullo.android.util.adapters.RecyclerItemClickListener;
import net.kullo.libkullo.api.Address;

public class ParticipantsAdapter extends AddressesAdapter<ParticipantsViewHolder> {

    @NonNull private final Context mContext;

    @Nullable private RecyclerItemClickListener mOnClickListener;

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

    public void setOnClickListener(RecyclerItemClickListener onClickListener) {
        mOnClickListener = onClickListener;
    }

    @Override
    public void onBindViewHolder(final ParticipantsViewHolder holder, int position) {
        Address participant = getItem(position);
        holder.mParticipantAddress.setText(participant.toString());

        if (isSelected(participant)) {
            holder.itemView.setBackgroundColor(mContext.getResources().getColor(R.color.kulloSelectionColor));
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = holder.getAdapterPosition();
                if (mOnClickListener != null) mOnClickListener.onClick(v, pos);
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @SuppressWarnings("SimplifiableIfStatement")
            @Override
            public boolean onLongClick(View v) {
                int pos = holder.getAdapterPosition();
                if (mOnClickListener != null) return mOnClickListener.onLongClick(v, pos);
                else return false;
            }
        });
    }
}
