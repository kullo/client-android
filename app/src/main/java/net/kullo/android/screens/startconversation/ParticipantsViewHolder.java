/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
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
