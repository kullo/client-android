/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.screens.sharereceiver;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.kullo.android.R;

class FileViewHolder extends RecyclerView.ViewHolder {

    final TextView mFilename;
    final TextView mFilesize;

    FileViewHolder(View itemView) {
        super(itemView);
        mFilename = (TextView) itemView.findViewById(R.id.filename);
        mFilesize = (TextView) itemView.findViewById(R.id.filesize);
    }
}
