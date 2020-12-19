/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.screens.compose;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.kullo.android.R;
import net.kullo.android.util.adapters.KulloIdsAdapter;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.Formatting;
import net.kullo.android.ui.ClickableRecyclerViewViewHolder;

public class DraftAttachmentsAdapter extends KulloIdsAdapter<DraftAttachmentsViewHolder> {
    private Context mContext;
    private Long mConversationId;
    private DraftAttachmentOpener mDraftAttachmentOpener;

    public interface OnItemClickListener {
        public void onItemClicked(int position, long id);
    }

    public interface OnItemLongClickListener {
        public boolean onItemLongClicked(int position, long id);
    }

    @Nullable public OnItemClickListener onItemClickListener = null;
    @Nullable public OnItemLongClickListener onItemLongClickListener = null;

    public DraftAttachmentsAdapter(Context context, Long conversationId, DraftAttachmentOpener draftAttachmentOpener) {
        super();
        mContext = context;
        mConversationId = conversationId;
        mDraftAttachmentOpener = draftAttachmentOpener;
        replaceAll(SessionConnector.get().getAttachmentsForDraft(conversationId));
    }

    @Override
    public DraftAttachmentsViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater
                .from(viewGroup.getContext())
                .inflate(R.layout.row_attachment, viewGroup, false);

        return new DraftAttachmentsViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(DraftAttachmentsViewHolder attachmentsViewHolder, final int position) {
        final long attachmentId = getItem(position);

        String filename = SessionConnector.get().getDraftAttachmentFilename(mConversationId, attachmentId);
        String sizeText = Formatting.filesizeHuman(SessionConnector.get().getDraftAttachmentFilesize(mConversationId, attachmentId));
        attachmentsViewHolder.mFilename.setText(filename);
        attachmentsViewHolder.mFilesize.setText(sizeText);

        attachmentsViewHolder.bindOnClickListener(new ClickableRecyclerViewViewHolder.OnItemClickListener() {
            @Override
            public void onItemClicked(int adapterPosition) {
                if (onItemClickListener != null) {
                    long attachmentId = getItem(adapterPosition);
                    onItemClickListener.onItemClicked(adapterPosition, attachmentId);
                }
            }
        });
        attachmentsViewHolder.bindOnItemLongClickListener(new ClickableRecyclerViewViewHolder.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClicked(int adapterPosition) {
                if (onItemLongClickListener != null) {
                    long attachmentId = getItem(adapterPosition);
                    return onItemLongClickListener.onItemLongClicked(adapterPosition, attachmentId);
                }
                return false;
            }
        });

        if (isSelected(attachmentId)) {
            attachmentsViewHolder.itemView.setBackgroundColor(mContext.getResources().getColor(R.color.kulloSelectionColor));
        } else {
            attachmentsViewHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }
}
