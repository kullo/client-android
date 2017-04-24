/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.messageslist;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.kullo.android.R;
import net.kullo.android.application.KulloApplication;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.thirdparty.EllipsizingTextView;
import net.kullo.android.util.adapters.KulloIdsAdapter;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class MessagesAdapter extends KulloIdsAdapter<MessagesViewHolder> {
    public interface FullyVisibleCallback {
        void onUnreadAndFullyVisible(Long item);
        void onNotUnreadAndFullyVisible(Long item);
    }

    public static final String TAG = "MessagesAdapter";
    private static final DateTimeZone LOCAL_TIME_ZONE = DateTimeZone.getDefault();
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private final DateTimeFormatter mFormatterClock;
    private final DateTimeFormatter mFormatterCalendarDate;
    private final Activity mBaseActivity;
    private boolean mShowCardsExpanded;

    @Nullable private FullyVisibleCallback mFullyVisibleCallback;

    public MessagesAdapter(final Activity baseActivity) {
        super();
        mBaseActivity = baseActivity;
        mShowCardsExpanded = false;

        mFormatterCalendarDate = ((KulloApplication) mBaseActivity.getApplication()).getShortDateFormatter();
        mFormatterClock = ((KulloApplication) mBaseActivity.getApplication()).getShortTimeFormatter();
    }

    public void toggleMessageSize() {
        mShowCardsExpanded = !mShowCardsExpanded;
        notifyDataSetChanged();
    }

    public boolean isShowCardsExpanded() {
        return mShowCardsExpanded;
    }

    private void onUnreadAndFullyVisible(Long item) {
        if (mFullyVisibleCallback != null) mFullyVisibleCallback.onUnreadAndFullyVisible(item);
    }

    private void onNotUnreadAndFullyVisible(Long item) {
        if (mFullyVisibleCallback != null) mFullyVisibleCallback.onNotUnreadAndFullyVisible(item);
    }

    public void setFullyVisibleCallback(@Nullable FullyVisibleCallback callback) {
        mFullyVisibleCallback = callback;
    }

    @Override
    public MessagesViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.row_message_preview, viewGroup, false);

        return new MessagesViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MessagesViewHolder messagesViewHolder, final int position) {
        final Long messageId = getItem(position);

        final boolean unread = SessionConnector.get().getMessageUnread(messageId);
        final DateTime dateReceived = SessionConnector.get().getMessageDateReceived(messageId);
        final String text = SessionConnector.get().getMessageText(messageId);
        final Bitmap senderAvatar = SessionConnector.get().getSenderAvatar(mBaseActivity, messageId);
        final String senderName = SessionConnector.get().getSenderName(messageId);
        final ArrayList<Long> attachmentIds = SessionConnector.get().getMessageAttachmentsIds(messageId);
        final boolean hasAttachments = !attachmentIds.isEmpty();

        messagesViewHolder.mMessageDateTextView.setText(getDateText(dateReceived));
        messagesViewHolder.mSenderAvatarImage.setImageBitmap(senderAvatar);
        messagesViewHolder.mSenderNameTextView.setText(senderName);
        messagesViewHolder.mUnreadIcon.setVisibility(unread ? View.VISIBLE : View.GONE);
        messagesViewHolder.mHasAttachmentsIcon.setVisibility(hasAttachments ? View.VISIBLE : View.GONE);

        messagesViewHolder.mMessageTextTextView.prepareForReuse();

        if (mShowCardsExpanded) {
            messagesViewHolder.mMessageTextTextView.setMaxLines(10);
            messagesViewHolder.mMessageTextTextView.setText(text);
        } else {
            final String textCompressed = WHITESPACE_PATTERN.matcher(text).replaceAll(" ");
            messagesViewHolder.mMessageTextTextView.setMaxLines(2);
            messagesViewHolder.mMessageTextTextView.setText(textCompressed);
        }

        messagesViewHolder.mMessageTextTextView.addEllipsizeListener(new EllipsizingTextView.EllipsizeListener() {
            @Override
            public void ellipsizeStateChanged(boolean ellipsized) {
                boolean messageIsUnreadAndFullyVisible = unread && !hasAttachments && !ellipsized;
                if (messageIsUnreadAndFullyVisible) {
                    onUnreadAndFullyVisible(messageId);
                } else {
                    onNotUnreadAndFullyVisible(messageId);
                }
            }
        });

        if (isSelected(messageId)) {
            messagesViewHolder.itemView.setBackgroundColor(mBaseActivity.getResources().getColor(R.color.kulloSelectionColor));
        } else {
            messagesViewHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private String getDateText(DateTime dateReceived) {
        LocalDateTime localDateReceived = new LocalDateTime(dateReceived, LOCAL_TIME_ZONE);

        String dateString;
        if(localDateReceived.toLocalDate().equals(new LocalDate())) {
            dateString = localDateReceived.toString(mFormatterClock);
        } else if(localDateReceived.toLocalDate().equals((new LocalDate()).minusDays(1))) {
            dateString = mBaseActivity.getResources().getString(R.string.yesterday);
        } else {
            dateString = localDateReceived.toString(mFormatterCalendarDate);
        }

        return dateString;
    }
}
