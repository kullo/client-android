/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.conversationslist;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter;

import net.kullo.android.R;
import net.kullo.android.application.KulloApplication;
import net.kullo.android.kulloapi.ConversationData;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.kulloapi.KulloIdsAdapter;
import net.kullo.android.littlehelpers.AvatarUtils;
import net.kullo.javautils.RuntimeAssertion;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormatter;

public class ConversationsAdapter extends KulloIdsAdapter<ConversationViewHolder>
        implements StickyRecyclerHeadersAdapter<RecyclerView.ViewHolder> {
    public static final String TAG = "ConversationsAdapter";
    public static final DateTimeZone LOCAL_TIME_ZONE = DateTimeZone.getDefault();

    private final DateTimeFormatter mFormatterCalendarDate;

    private Activity mBaseActivity;

    public ConversationsAdapter(Activity activity) {
        super();
        mBaseActivity = activity;
        mFormatterCalendarDate = ((KulloApplication) mBaseActivity.getApplication()).getShortDateFormatter();
    }

    @Override
    public ConversationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_conversation, parent, false);
        return new ConversationViewHolder(view) {
        };
    }

    @Override
    public void onBindViewHolder(ConversationViewHolder holder, int position) {
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

        // draw mark if conversation contains unread messages
        if (data.mCountUnread > 0) {
            holder.mUnreadMark.setVisibility(View.VISIBLE);
        } else {
            holder.mUnreadMark.setVisibility(View.GONE);
        }
    }

    @Override
    public long getHeaderId(int position) {
        long conversationId = getItem(position);
        DateTime latestMessageTimestamp = SessionConnector.get().getLatestMessageTimestamp(conversationId);

        if (latestMessageTimestamp.equals(SessionConnector.get().emptyConversationTimestamp())) {
            // maximum positive value for a 64-bit signed integer
            return 0x7fffffffffffffffL;
        } else {
            LocalDateTime localLatestMessageTimestamp = new LocalDateTime(latestMessageTimestamp, LOCAL_TIME_ZONE);
            LocalDate today = new LocalDate(LOCAL_TIME_ZONE);

            // The header id must change whenever the date of the latest message changed (t1)
            // as well as when the current date changes (t2) to invalidate "today" or "yesterday" labels.
            // This is going to work until year 2038, then we need to cut non-significant bits.
            long t1 = localLatestMessageTimestamp.toLocalDate().toDateTimeAtStartOfDay().getMillis() / 1000; // 31 bit
            long t2 = today.toDateTimeAtStartOfDay().getMillis() / 1000; // 31 bit
            return (t1 << 31) | t2;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_conversation_header, parent, false);
        return new RecyclerView.ViewHolder(view) {
        };
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder, int position) {
        TextView textView = (TextView) holder.itemView;
        Long conversationId = getItem(position);

        DateTime latestMessageTimestamp = SessionConnector.get().getLatestMessageTimestamp(conversationId);

        if (latestMessageTimestamp.getMillis() == SessionConnector.get().emptyConversationTimestamp().getMillis()) {
            textView.setText(R.string.empty_conversation_title);
        }
        else {
            textView.setText(getLocalDateText(latestMessageTimestamp));
        }
    }

    private String getLocalDateText(DateTime dateLatestMessage) {
        LocalDateTime localDateReceived = new LocalDateTime(dateLatestMessage, LOCAL_TIME_ZONE);

        String dateString;
        if(localDateReceived.toLocalDate().equals(new LocalDate())) {
            dateString = mBaseActivity.getResources().getString(R.string.today);
        } else if(localDateReceived.toLocalDate().equals((new LocalDate()).minusDays(1))) {
            dateString = mBaseActivity.getResources().getString(R.string.yesterday);
        } else {
            dateString = localDateReceived.toString(mFormatterCalendarDate);
        }

        return dateString;
    }
}
