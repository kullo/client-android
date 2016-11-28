/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.compose;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.kullo.android.R;
import net.kullo.android.kulloapi.KulloIdsAdapter;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.Formatting;

public class DraftAttachmentsAdapter extends KulloIdsAdapter<DraftAttachmentsViewHolder> {
    private Context mContext;
    private Long mConversationId;
    private DraftAttachmentOpener mDraftAttachmentOpener;

    public DraftAttachmentsAdapter(Context context, Long conversationId, DraftAttachmentOpener draftAttachmentOpener) {
        super();
        mContext = context;
        mConversationId = conversationId;
        mDraftAttachmentOpener = draftAttachmentOpener;
        replaceAll(SessionConnector.get().getAttachmentsForDraft(conversationId));
    }

    @Override
    public DraftAttachmentsViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.row_draft_attachment, viewGroup, false);

        return new DraftAttachmentsViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(DraftAttachmentsViewHolder attachmentsViewHolder, final int position) {
        final long attachmentId = getItem(position);

        String filename = SessionConnector.get().getDraftAttachmentFilename(mConversationId, attachmentId);
        String sizeText = Formatting.filesizeHuman(SessionConnector.get().getDraftAttachmentFilesize(mConversationId, attachmentId));
        String text = filename + " (" + sizeText + ")";
        attachmentsViewHolder.mAttachmentName.setText(text);

        int textColor = mContext.getResources().getColor(R.color.kulloTextPrimaryColor);
        attachmentsViewHolder.mAttachmentName.setTextColor(textColor);

        attachmentsViewHolder.mRemoveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SessionConnector.get().removeDraftAttachment(mConversationId, attachmentId);
            }
        });

        attachmentsViewHolder.mAttachmentName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDraftAttachmentOpener.saveAndOpenAttachment(mConversationId, attachmentId);
            }
        });
    }
}
