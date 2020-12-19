/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.screens.singlemessage;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.kullo.android.R;
import net.kullo.javautils.RuntimeAssertion;

class AttachmentsViewHolder extends RecyclerView.ViewHolder {
    @NonNull final View mContainer;
    @NonNull final TextView mFilename;
    @NonNull final TextView mFilesize;
    @NonNull final ImageView mIcon;
    @NonNull final ImageView mImagePreview;

    AttachmentsViewHolder(View itemView) {
        super(itemView);
        mContainer = itemView.findViewById(R.id.container);
        mFilename = (TextView) itemView.findViewById(R.id.filename);
        mFilesize = (TextView) itemView.findViewById(R.id.filesize);
        mIcon = (ImageView) itemView.findViewById(R.id.icon_default);
        mImagePreview = (ImageView) itemView.findViewById(R.id.image_preview);
        RuntimeAssertion.require(mContainer != null);
        RuntimeAssertion.require(mFilename != null);
        RuntimeAssertion.require(mFilesize != null);
        RuntimeAssertion.require(mIcon != null);
        RuntimeAssertion.require(mImagePreview != null);
    }
}
