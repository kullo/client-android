/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
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
