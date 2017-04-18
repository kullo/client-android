/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.conversationslist;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.kullo.android.R;
import net.kullo.android.kulloapi.ConversationData;
import net.kullo.android.util.adapters.KulloIdsAdapter;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.AvatarUtils;
import net.kullo.javautils.RuntimeAssertion;

import org.joda.time.DateTimeZone;

import java.util.ArrayList;

public class ConversationsAdapter extends KulloIdsAdapter<ConversationViewHolder> {
    public static final String TAG = "ConversationsAdapter";
    public static final DateTimeZone LOCAL_TIME_ZONE = DateTimeZone.getDefault();

    private Activity mBaseActivity;

    public ConversationsAdapter(Activity activity) {
        super();
        mBaseActivity = activity;
    }

    @Override
    public ConversationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ConversationViewHolder holder, int position) {
        final long conversationId = getItem(position);
        ConversationData data = SessionConnector.get().getConversationData(mBaseActivity, conversationId);

        holder.mConversationName.setText(data.title);

        int pixelSize = mBaseActivity.getResources().getDimensionPixelSize(R.dimen.md_additions_list_avatar_size);
        Bitmap combinedAvatar = AvatarUtils.combine(
            new ArrayList<>(data.participantsAvatar.values()),
            pixelSize);
        RuntimeAssertion.require(combinedAvatar != null);
        holder.mAvatarImage.setImageBitmap(combinedAvatar);

        // draw background if item is selected
        if (isSelected(conversationId)) {
            holder.mBaseView.setBackgroundColor(mBaseActivity.getResources().getColor(R.color.kulloSelectionColor));
        } else {
            holder.mBaseView.setBackgroundColor(Color.TRANSPARENT);
        }

        // draw mark if conversation contains unread messages
        if (data.countUnread > 0) {
            holder.mUnreadMark.setVisibility(View.VISIBLE);
        } else {
            holder.mUnreadMark.setVisibility(View.GONE);
        }
    }
}
