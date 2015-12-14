/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.conversationslist;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.kullo.android.R;

import de.hdodenhof.circleimageview.CircleImageView;

public class ConversationViewHolder extends RecyclerView.ViewHolder {
    protected CircleImageView mAvatarImage;
    protected TextView mConversationName;
    protected View mBaseView;
    protected ImageView mUnreadMark;

    public ConversationViewHolder(View itemView) {
        super(itemView);
        mBaseView = itemView;
        mAvatarImage = (CircleImageView) itemView.findViewById(R.id.img_avatar);
        mConversationName = (TextView) itemView.findViewById(R.id.conversation_name);
        mUnreadMark = (ImageView) itemView.findViewById(R.id.unread_icon);
    }
}
