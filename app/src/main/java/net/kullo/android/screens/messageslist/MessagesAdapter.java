/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.messageslist;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.UiThread;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.kullo.android.R;
import net.kullo.android.application.KulloApplication;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.kulloapi.KulloIdsAdapter;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;

// import android.util.Log;

public class MessagesAdapter extends KulloIdsAdapter<MessagesViewHolder> {
    public static final String TAG = "MessagesAdapter";
    public static final DateTimeZone LOCAL_TIME_ZONE = DateTimeZone.getDefault();

    final protected DateTimeFormatter mFormatterClock;
    final protected DateTimeFormatter mFormatterCalendarDate;
    final protected Activity mBaseActivity;
    final protected Long mConversationId;
	private boolean mShowCardsExpanded;

    public MessagesAdapter(final Activity baseActivity, Long conversationId, MessageAttachmentsOpener messageAttachmentsOpener) {
        super();
        mBaseActivity = baseActivity;
        mConversationId = conversationId;
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
        final String textCompressed = text.replaceAll("\\s+", " ");
        final Bitmap senderAvatar = SessionConnector.get().getSenderAvatar(mBaseActivity, messageId);
        final String senderName = SessionConnector.get().getSenderName(messageId);
        final ArrayList<Long> attachmentIds = SessionConnector.get().getMessageAttachmentsIds(messageId);

        messagesViewHolder.mMessageDateTextView.setText(getDateText(dateReceived));
        messagesViewHolder.mSenderAvatarImage.setImageBitmap(senderAvatar);
        messagesViewHolder.mSenderNameTextView.setText(senderName);
        messagesViewHolder.mUnreadIcon.setVisibility(unread ? View.VISIBLE : View.GONE);
        messagesViewHolder.mHasAttachmentsIcon.setVisibility(!attachmentIds.isEmpty() ? View.VISIBLE : View.GONE);

        if (mShowCardsExpanded) {
            messagesViewHolder.mMessageTextTextView.setMaxLines(Integer.MAX_VALUE);
            messagesViewHolder.mMessageTextTextView.setAutoLinkMask(Linkify.WEB_URLS);
            messagesViewHolder.mMessageTextTextView.setText(text);
        } else {
            messagesViewHolder.mMessageTextTextView.setMaxLines(2);
            messagesViewHolder.mMessageTextTextView.setAutoLinkMask(0);
            messagesViewHolder.mMessageTextTextView.setText(textCompressed);
        }

        // draw background if item is selected
        if (isSelected(messageId)) {
            messagesViewHolder.itemView.setBackgroundColor(mBaseActivity.getResources().getColor(R.color.kulloSelectionColor));
        } else {
            messagesViewHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void addMessageIds() {
        replaceAll(SessionConnector.get().getAllMessageIdsSorted(mConversationId));
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

    @UiThread
    public void updateDataSet() {
        addMessageIds();
        notifyDataSetChanged();
    }
}
