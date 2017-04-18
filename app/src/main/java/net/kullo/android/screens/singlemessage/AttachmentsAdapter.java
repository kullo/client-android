/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.singlemessage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.UiThread;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.BitmapEncoder;
import com.bumptech.glide.load.resource.bytes.BytesResource;

import net.kullo.android.R;
import net.kullo.android.util.adapters.KulloIdsAdapter;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.kulloapi.attachments.AttachmentFromCacheDecoder;
import net.kullo.android.kulloapi.attachments.AttachmentFromDatabaseLoader;
import net.kullo.android.kulloapi.attachments.AttachmentFromOriginalDecoder;
import net.kullo.android.kulloapi.attachments.AttachmentIdentifier;
import net.kullo.android.littlehelpers.Formatting;
import net.kullo.android.littlehelpers.Images;

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
    public void onBindViewHolder(final AttachmentsViewHolder attachmentsViewHolder, int position) {
        final long attachmentId = getItem(position);

        final String filename = SessionConnector.get().getMessageAttachmentFilename(mMessageId, attachmentId);
        final String mimeType = SessionConnector.get().getMessageAttachmentMimeType(mMessageId, attachmentId);
        final long size = SessionConnector.get().getMessageAttachmentFilesize(mMessageId, attachmentId);
        final String sizeText = Formatting.filesizeHuman(size);

        final AttachmentIdentifier attachment = new AttachmentIdentifier();
        attachment.messageId = mMessageId;
        attachment.attachmentId = attachmentId;

        attachmentsViewHolder.mFilename.setText(filename);
        attachmentsViewHolder.mFilesize.setText(sizeText);

        if (!mAttachmentsDownloaded) {
            attachmentsViewHolder.mContainer.setAlpha(0.5f);
        } else {
            attachmentsViewHolder.mContainer.setAlpha(1.0f);
        }

        if (mAttachmentsDownloaded && Images.THUMBNAILABLE_TYPES.contains(mimeType)) {
            attachmentsViewHolder.mIcon.setVisibility(View.GONE);
            attachmentsViewHolder.mImagePreview.setVisibility(View.VISIBLE);

            // make sure ImageView is layed out
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    Glide.with(mContext)
                        .using(new AttachmentFromDatabaseLoader(), BytesResource.class)
                        .load(attachment)
                        .as(Bitmap.class)
                        .decoder(new AttachmentFromOriginalDecoder(mContext))
                        .encoder(new BitmapEncoder())
                        .cacheDecoder(new AttachmentFromCacheDecoder(mContext))
                        .into(attachmentsViewHolder.mImagePreview);
                }
            });
        } else {
            attachmentsViewHolder.mIcon.setVisibility(View.VISIBLE);
            attachmentsViewHolder.mImagePreview.setVisibility(View.GONE);
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
