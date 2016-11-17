/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.sharereceiver;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.kullo.android.R;
import net.kullo.android.littlehelpers.Formatting;

import java.util.ArrayList;

public class ImagesPreviewAdapter extends RecyclerView.Adapter<ImagesPreviewViewHolder> {

    private ArrayList<Share> mShares = new ArrayList<>();

    @Override
    public ImagesPreviewViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.single_image, parent, false);
        return new ImagesPreviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ImagesPreviewViewHolder holder, int position) {
        Share share = mShares.get(position);
        holder.mImage.setImageBitmap(share.preview);
        String metaText = String.format("%s (%s)",
                share.filename, Formatting.filesizeHuman(share.size));
        holder.mImageMetaText.setText(metaText);
    }

    @Override
    public int getItemCount() {
        return mShares.size();
    }

    public void add(Share share) {
        mShares.add(share);
        int newItemPosition = mShares.size()-1;
        notifyItemInserted(newItemPosition);
    }
}
