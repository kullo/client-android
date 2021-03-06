/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.screens.messageslist;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.kullo.android.R;
import net.kullo.android.thirdparty.EllipsizingTextView;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessagesViewHolder extends RecyclerView.ViewHolder {
    CircleImageView mSenderAvatarImage;
    TextView mMessageDateTextView;
    TextView mSenderNameTextView;
    EllipsizingTextView mMessageTextTextView;
    final ImageView mUnreadIcon;
    final ImageView mHasAttachmentsIcon;

    MessagesViewHolder(View itemView) {
        super(itemView);

        mSenderAvatarImage = (CircleImageView) itemView.findViewById(R.id.img_sender);
        mSenderNameTextView = (TextView) itemView.findViewById(R.id.sender_name_organization);
        mMessageDateTextView = (TextView) itemView.findViewById(R.id.date);
        mMessageTextTextView = (EllipsizingTextView) itemView.findViewById(R.id.message_content);
        mUnreadIcon = (ImageView) itemView.findViewById(R.id.unread_icon);
        mHasAttachmentsIcon = (ImageView) itemView.findViewById(R.id.has_attachments_icon);

        mMessageTextTextView.setAutoLinkMask(0);
    }
}
