/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

import android.widget.Filterable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.content.Context;
import android.widget.Filter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.kullo.android.R;


public class AddressAutocompleteAdapter extends ArrayAdapter<String> implements Filterable {
        private LayoutInflater mInflater;
        private final List<String> mDomains;

        private static final String HASH_CHAR = "#";

        public AddressAutocompleteAdapter(final Context context) {
            super(context, -1);
            mInflater = LayoutInflater.from(context);
            // Right now just one domain in the list
            mDomains = Arrays.asList(context.getString(R.string.kullo_domain));
        }

        @Override
        public View getView(final int position, final View view, final ViewGroup parentView) {
            final TextView newView;
            if (view == null) {
                newView = (TextView)mInflater.inflate(android.R.layout.simple_dropdown_item_1line, parentView, false);
            } else {
                newView = (TextView)view;
            }
            newView.setText(getItem(position));
            return newView;
        }


        @Override
        public Filter getFilter() {
            return (new Filter() {
                @Override
                protected void publishResults(final CharSequence contraint, final FilterResults filterResults) {
                    final ArrayList<String> suggestions = (ArrayList<String>)filterResults.values;
                    clear();
                    for (String suggestion : suggestions) {
                        add(suggestion);
                    }
                    notifyDataSetChanged();
                }

                @Override
                protected FilterResults performFiltering(final CharSequence constraint) {
                    final ArrayList<String> suggestions = new ArrayList<String>();

                    if (constraint != null) {
                        for (String domainName : mDomains) {
                            String result = appendDomain((String)constraint, domainName);
                            if (result != null) {
                                suggestions.add(result);
                            }
                        }
                    }

                    final FilterResults filterResults = new FilterResults();
                    filterResults.values = suggestions;
                    filterResults.count = suggestions.size();

                    return filterResults;
                }

                private String  appendDomain(final String input, final String domainName) {
                    String result = null;
                    final int hashIndex = input.indexOf(HASH_CHAR);
                    if (hashIndex != -1) {
                        String domainBegin = domainName.substring(0, Math.min(input.length() - hashIndex, domainName.length()));
                        if (input.endsWith(domainBegin)) {
                            result = input.substring(0,hashIndex) + domainName;
                        }
                    }
                    return result;
                }
            });
        }
    }
