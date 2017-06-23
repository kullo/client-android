/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.search;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.kullo.android.R;
import net.kullo.android.thirdparty.NullViewHolder;

public class SearchAdapter
    extends net.kullo.android.thirdparty.StaticSectionsAdapter<SearchResultViewHolder, SearchHeaderViewHolder, NullViewHolder> {

    public SearchAdapter(RecyclerView.Adapter<SearchResultViewHolder> itemsAdapters) {
        super(itemsAdapters);
    }

    @Override
    protected boolean sectionHasHeader(int sectionIndex) {
        return true;
    }

    @Override
    protected boolean sectionHasFooter(int sectionIndex) {
        return false;
    }

    @Override
    protected SearchHeaderViewHolder onCreateSectionHeaderViewHolder(ViewGroup parent) {
        View itemView = LayoutInflater.
            from(parent.getContext()).
            inflate(R.layout.row_search_header, parent, false);
        return new SearchHeaderViewHolder(itemView);
    }

    @Override
    protected NullViewHolder onCreateSectionFooterViewHolder(ViewGroup parent) {
        return null;
    }

    @Override
    protected void onBindHeaderViewHolder(SearchHeaderViewHolder vh, int sectionIndex) {
        vh.setValues();
    }

    @Override
    protected void onBindFooterViewHolder(NullViewHolder vh, int sectionIndex) {
    }
}
