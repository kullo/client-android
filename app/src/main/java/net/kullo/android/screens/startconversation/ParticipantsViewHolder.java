/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.startconversation;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import net.kullo.android.R;

public class ParticipantsViewHolder extends RecyclerView.ViewHolder {
    protected TextView mParticipantAddress;

    public ParticipantsViewHolder(View itemView) {
        super(itemView);
        mParticipantAddress = (TextView) itemView.findViewById(R.id.participant_address);
    }
}
