/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
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
