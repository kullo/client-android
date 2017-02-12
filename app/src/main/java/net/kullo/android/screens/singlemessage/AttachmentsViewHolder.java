/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.singlemessage;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import net.kullo.android.R;
import net.kullo.javautils.RuntimeAssertion;

class AttachmentsViewHolder extends RecyclerView.ViewHolder {
    View mContainer;
    TextView mFilename;
    TextView mFilesize;

    AttachmentsViewHolder(View itemView) {
        super(itemView);
        mContainer = itemView.findViewById(R.id.container);
        mFilename = (TextView) itemView.findViewById(R.id.filename);
        mFilesize = (TextView) itemView.findViewById(R.id.filesize);
        RuntimeAssertion.require(mContainer != null);
        RuntimeAssertion.require(mFilename != null);
        RuntimeAssertion.require(mFilesize != null);
    }
}
