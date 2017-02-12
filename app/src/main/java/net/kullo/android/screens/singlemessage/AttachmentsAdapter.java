/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.singlemessage;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.UiThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.kullo.android.R;
import net.kullo.android.kulloapi.KulloIdsAdapter;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.Formatting;

public class AttachmentsAdapter extends KulloIdsAdapter<AttachmentsViewHolder> {
    private Context mContext;
    private long mMessageId;
    private boolean mAttachmentsDownloaded;

    public AttachmentsAdapter(Context context, long messageId, boolean attachmentsDownloaded) {
        super();
        mContext = context;
        mMessageId = messageId;
        mAttachmentsDownloaded = attachmentsDownloaded;
        replaceAll(SessionConnector.get().getMessageAttachmentsIds(mMessageId));
    }

    @Override
    public AttachmentsViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater
                .from(viewGroup.getContext())
                .inflate(R.layout.row_attachment, viewGroup, false);

        return new AttachmentsViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(AttachmentsViewHolder attachmentsViewHolder, int position) {
        final long attachmentId = getItem(position);

        String filename = SessionConnector.get().getMessageAttachmentFilename(mMessageId, attachmentId);
        String sizeText = Formatting.filesizeHuman(SessionConnector.get().getMessageAttachmentFilesize(mMessageId, attachmentId));
        attachmentsViewHolder.mFilename.setText(filename);
        attachmentsViewHolder.mFilesize.setText(sizeText);

        if (!mAttachmentsDownloaded) {
            attachmentsViewHolder.mContainer.setAlpha(0.5f);
        } else {
            attachmentsViewHolder.mContainer.setAlpha(1.0f);
        }

        if (isSelected(attachmentId)) {
            attachmentsViewHolder.itemView.setBackgroundColor(mContext.getResources().getColor(R.color.kulloSelectionColor));
        } else {
            attachmentsViewHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @UiThread
    protected void dataSetChanged() {
        notifyDataSetChanged();
    }
}
