/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.sharereceiver;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.kullo.android.R;

class ImagesPreviewViewHolder extends RecyclerView.ViewHolder {
    final ImageView mImage;
    final TextView mImageMetaText;

    ImagesPreviewViewHolder(View itemView) {
        super(itemView);
        mImage = (ImageView) itemView.findViewById(R.id.main_image);
        mImageMetaText = (TextView) itemView.findViewById(R.id.image_meta_text);
    }
}
