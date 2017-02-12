/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.sharereceiver;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.kullo.android.R;
import net.kullo.android.kulloapi.ConversationData;
import net.kullo.android.kulloapi.KulloIdsAdapter;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.AvatarUtils;
import net.kullo.javautils.RuntimeAssertion;

public class ShareTargetsAdapter extends KulloIdsAdapter<ShareTargetViewHolder> {
    public static final String TAG = "ShareTargetsAdapter";

    private Activity mBaseActivity;

    public ShareTargetsAdapter(Activity activity) {
        super();
        mBaseActivity = activity;
    }

    @Override
    public ShareTargetViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_conversation, parent, false);
        return new ShareTargetViewHolder(view) {
        };
    }

    @Override
    public void onBindViewHolder(ShareTargetViewHolder holder, final int position) {
        final long conversationId = getItem(position);
        ConversationData data = SessionConnector.get().getConversationData(mBaseActivity, conversationId);

        holder.mConversationName.setText(data.mTitle);

        int pixelSize = mBaseActivity.getResources().getDimensionPixelSize(R.dimen.md_additions_list_avatar_size);
        Bitmap combinedAvatar = AvatarUtils.combine(data.mParticipantsAvatars, pixelSize);
        RuntimeAssertion.require(combinedAvatar != null);
        holder.mAvatarImage.setImageBitmap(combinedAvatar);

        // draw background if item is selected
        if (isSelected(conversationId)) {
            holder.mBaseView.setBackgroundColor(mBaseActivity.getResources().getColor(R.color.kulloSelectionColor));
        } else {
            holder.mBaseView.setBackgroundColor(Color.TRANSPARENT);
        }

        holder.mUnreadMark.setVisibility(View.GONE);
    }
}
