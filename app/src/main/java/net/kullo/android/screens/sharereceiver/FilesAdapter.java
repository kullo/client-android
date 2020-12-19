/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.screens.sharereceiver;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.kullo.android.R;
import net.kullo.android.littlehelpers.Formatting;

import java.util.ArrayList;

public class FilesAdapter extends RecyclerView.Adapter<FileViewHolder> {

    private ArrayList<Share> mShares = new ArrayList<>();

    @Override
    public FileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.single_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FileViewHolder holder, int position) {
        Share share = mShares.get(position);
        holder.mFilename.setText(share.filename);
        holder.mFilesize.setText(Formatting.filesizeHuman(share.size));
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
