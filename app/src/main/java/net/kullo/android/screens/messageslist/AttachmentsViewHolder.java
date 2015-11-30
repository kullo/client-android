/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.messageslist;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import net.kullo.android.R;

public class AttachmentsViewHolder extends RecyclerView.ViewHolder {
    protected TextView mAttachmentName;

    public AttachmentsViewHolder(View itemView) {
        super(itemView);
        mAttachmentName = (TextView) itemView.findViewById(R.id.attachmentName);
    }
}
