/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.screens.messageslist;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.kullo.android.R;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.AddressSet;
import net.kullo.android.thirdparty.NullViewHolder;

public class ConversationAdapter
    extends net.kullo.android.thirdparty.StaticSectionsAdapter<MessagesViewHolder, ConversationHeaderViewHolder, NullViewHolder> {

    private final long mConversationId;

    public ConversationAdapter(RecyclerView.Adapter<MessagesViewHolder> itemsAdapters, long conversationId) {
        super(itemsAdapters);
        mConversationId = conversationId;
    }

    @Override
    protected boolean sectionHasHeader(int sectionIndex) {
        return true;
    }

    @Override
    protected boolean sectionHasFooter(int sectionIndex) {
        return false;
    }

    @Override
    protected ConversationHeaderViewHolder onCreateSectionHeaderViewHolder(ViewGroup parent) {
        View itemView = LayoutInflater.
            from(parent.getContext()).
            inflate(R.layout.row_messages_list_header, parent, false);
        return new ConversationHeaderViewHolder(itemView);
    }

    @Override
    protected NullViewHolder onCreateSectionFooterViewHolder(ViewGroup parent) {
        return null;
    }

    @Override
    protected void onBindHeaderViewHolder(ConversationHeaderViewHolder vh, int sectionIndex) {
        AddressSet participants = SessionConnector.get().getParticipantAddresses(mConversationId);
        vh.setValues(participants);
    }

    @Override
    protected void onBindFooterViewHolder(NullViewHolder vh, int sectionIndex) {
    }
}
