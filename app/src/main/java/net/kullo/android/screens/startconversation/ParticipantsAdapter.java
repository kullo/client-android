/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.startconversation;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.kullo.android.R;
import net.kullo.android.littlehelpers.AddressSet;
import net.kullo.libkullo.api.Address;

public class ParticipantsAdapter extends RecyclerView.Adapter<ParticipantsViewHolder> {
    private AddressSet mParticipants;

    public ParticipantsAdapter() {
        mParticipants = new AddressSet();
    }

    @Override
    public ParticipantsViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.row_participant, viewGroup, false);

        return new ParticipantsViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ParticipantsViewHolder participantsViewHolder, int i) {
        Address participantAddress = mParticipants.sorted().get(i);
        participantsViewHolder.mParticipantAddress.setText(participantAddress.toString());
    }

    @Override
    public int getItemCount() {
        return mParticipants.size();
    }

    public AddressSet getItems() {
        return mParticipants;
    }

    public boolean add(Address address) {
        return mParticipants.add(address);
    }
}
