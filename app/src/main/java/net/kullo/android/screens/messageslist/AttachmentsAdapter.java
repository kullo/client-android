/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.messageslist;

import android.content.Context;
import android.support.annotation.UiThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.kullo.android.R;
import net.kullo.android.kulloapi.KulloIdsAdapter;
import net.kullo.android.kulloapi.KulloConnector;
import net.kullo.android.littlehelpers.Formatting;

public class AttachmentsAdapter extends KulloIdsAdapter<AttachmentsViewHolder> {
    private Context mContext;
    private Long mMessageId;
    private boolean mAttachmentsDownloaded;

    public AttachmentsAdapter(Context context, Long messageId, boolean attachmentsDownloaded) {
        super();
        mContext = context;
        mMessageId = messageId;
        mAttachmentsDownloaded = attachmentsDownloaded;
        replaceAll(KulloConnector.get().getMessageAttachmentsIds(mMessageId));
    }

    @Override
    public AttachmentsViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.row_attachment, viewGroup, false);

        return new AttachmentsViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(AttachmentsViewHolder attachmentsViewHolder, int position) {
        final long attachmentId = getItem(position);

        String filename = KulloConnector.get().getMessageAttachmentFilename(mMessageId, attachmentId);
        String sizeText = Formatting.filesizeHuman(KulloConnector.get().getMessageAttachmentFilesize(mMessageId, attachmentId));
        String text = filename + " (" + sizeText + ")";
        attachmentsViewHolder.mAttachmentName.setText(text);

        int textColor = mContext.getResources().getColor(R.color.kulloTextPrimaryColor);
        if (!mAttachmentsDownloaded) {
            textColor = mContext.getResources().getColor(R.color.kulloDisabledTextColor);
        }
        attachmentsViewHolder.mAttachmentName.setTextColor(textColor);
    }

    @UiThread
    protected void dataSetChanged() {
        notifyDataSetChanged();
    }
}
