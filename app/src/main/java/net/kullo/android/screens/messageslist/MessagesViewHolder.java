/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.messageslist;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.kullo.android.R;
import net.kullo.android.thirdparty.EllipsizingTextView;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessagesViewHolder extends RecyclerView.ViewHolder {
    protected View mItemView;
    protected CircleImageView mSenderAvatarImage;
    protected TextView mMessageDateTextView;
    protected TextView mSenderNameTextView;
    protected EllipsizingTextView mMessageTextTextView;
    protected final ImageView mUnreadIcon;
    protected final ImageView mHasAttachmentsIcon;

    public MessagesViewHolder(View itemView) {
        super(itemView);

        mItemView = itemView;
        mSenderAvatarImage = (CircleImageView) itemView.findViewById(R.id.img_sender);
        mSenderNameTextView = (TextView) itemView.findViewById(R.id.sender_name);
        mMessageDateTextView = (TextView) itemView.findViewById(R.id.message_date);
        mMessageTextTextView = (EllipsizingTextView) itemView.findViewById(R.id.message_content);
        mUnreadIcon = (ImageView) itemView.findViewById(R.id.unread_icon);
        mHasAttachmentsIcon = (ImageView) itemView.findViewById(R.id.has_attachments_icon);
    }
}
