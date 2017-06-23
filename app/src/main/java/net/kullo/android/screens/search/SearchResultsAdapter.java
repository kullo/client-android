/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.search;

import android.graphics.Bitmap;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.kullo.android.R;
import net.kullo.android.application.KulloApplication;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.Formatting;
import net.kullo.android.littlehelpers.TextViewContent;
import net.kullo.android.util.adapters.ReadableAdapter;
import net.kullo.android.util.adapters.RecyclerItemClickListener;
import net.kullo.libkullo.api.MessagesSearchResult;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collection;

public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultViewHolder>
    implements ReadableAdapter<MessagesSearchResult> {

    @Nullable private RecyclerItemClickListener mOnClickListener;

    private ArrayList<MessagesSearchResult> mStorage = new ArrayList<>();

    public SearchResultsAdapter() {
    }

    public void setOnClickListener(@Nullable RecyclerItemClickListener onClickListener) {
        mOnClickListener = onClickListener;
    }

    @MainThread
    public void reset(final ArrayList<MessagesSearchResult> results) {
        mStorage = results;
        notifyDataSetChanged();
    }

    @Override
    public SearchResultViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.row_search_result, parent, false);
        return new SearchResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final SearchResultViewHolder holder, int position) {
        final MessagesSearchResult result = mStorage.get(position);

        final Spannable snippet = TextViewContent.highlightSearchResult(result.getSnippet(), result.getBoundary());
        holder.snippet.setText(snippet);

        final long messageId = result.getMsgId();
        final DateTime dateReceived = SessionConnector.get().getMessageDateReceived(messageId);
        final String senderName = SessionConnector.get().getSenderName(messageId);
        final Bitmap senderAvatar = SessionConnector.get().getSenderAvatar(KulloApplication.sharedInstance, result.getMsgId());
        final ArrayList<Long> attachmentIds = SessionConnector.get().getMessageAttachmentsIds(messageId);
        final boolean hasAttachments = !attachmentIds.isEmpty();

        holder.senderAvatar.setImageBitmap(senderAvatar);
        holder.senderNameOrganization.setText(senderName);
        holder.date.setText(Formatting.shortDateText(dateReceived));
        holder.hasAttachments.setVisibility(hasAttachments ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = holder.getAdapterPosition();
                if (mOnClickListener != null) mOnClickListener.onClick(v, pos);
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @SuppressWarnings("SimplifiableIfStatement")
            @Override
            public boolean onLongClick(View v) {
                int pos = holder.getAdapterPosition();
                if (mOnClickListener != null) return mOnClickListener.onLongClick(v, pos);
                else return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mStorage.size();
    }

    @Override
    public int find(MessagesSearchResult item) {
        // no equality of search results
        return -1;
    }

    @Override
    public MessagesSearchResult getItem(int position) {
        return mStorage.get(position);
    }

    @Override
    public Collection<MessagesSearchResult> getItems() {
        return mStorage;
    }
}
