/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.search;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.kullo.android.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SearchResultViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.snippet) TextView snippet;
    @BindView(R.id.sender_avatar) ImageView senderAvatar;
    @BindView(R.id.sender_name_organization) TextView senderNameOrganization;
    @BindView(R.id.date) TextView date;
    @BindView(R.id.has_attachments) ImageView hasAttachments;

    public SearchResultViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}
