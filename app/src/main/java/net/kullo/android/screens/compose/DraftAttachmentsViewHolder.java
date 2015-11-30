/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.compose;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.kullo.android.R;

public class DraftAttachmentsViewHolder extends RecyclerView.ViewHolder {
    public TextView mAttachmentName;
    public ImageView mRemoveButton;

    public DraftAttachmentsViewHolder(View itemView) {
        super(itemView);
        mAttachmentName = (TextView) itemView.findViewById(R.id.draft_attachment_name);
        mRemoveButton = (ImageView) itemView.findViewById(R.id.draft_attachment_remove_button);
    }
}
