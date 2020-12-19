/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.screens.messageslist;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import net.kullo.android.R;
import net.kullo.android.application.KulloApplication;
import net.kullo.android.littlehelpers.AddressSet;

public class ConversationHeaderViewHolder extends RecyclerView.ViewHolder {

    private TextView mText;

    public ConversationHeaderViewHolder(View itemView) {
        super(itemView);
        mText = (TextView) itemView.findViewById(R.id.text);
    }

    public void setValues(AddressSet otherParticipants) {
        final String withText;
        if (otherParticipants.size() == 1) {
            withText = otherParticipants.sorted().get(0).toString();
        } else {
            withText = String.format(
                KulloApplication.sharedInstance.getString(R.string.conversation_info_teaser_with_x_participants),
                otherParticipants.size());
        }

        final String text = String.format(
            KulloApplication.sharedInstance.getString(R.string.conversation_info_teaser_conversation_with),
            withText);
        mText.setText(text);
    }
}
