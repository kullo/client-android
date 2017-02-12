/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.compose;

import android.view.View;
import android.widget.TextView;

import net.kullo.android.R;
import net.kullo.android.ui.ClickableRecyclerViewViewHolder;
import net.kullo.javautils.RuntimeAssertion;

class DraftAttachmentsViewHolder extends ClickableRecyclerViewViewHolder {
    TextView mFilename;
    TextView mFilesize;

    DraftAttachmentsViewHolder(View itemView) {
        super(itemView);

        mFilename = (TextView) itemView.findViewById(R.id.filename);
        mFilesize = (TextView) itemView.findViewById(R.id.filesize);
        RuntimeAssertion.require(mFilename != null);
        RuntimeAssertion.require(mFilesize != null);
    }

}
