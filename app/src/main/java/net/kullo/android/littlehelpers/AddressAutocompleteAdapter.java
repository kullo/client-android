/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.littlehelpers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.javautils.RuntimeAssertion;

import java.util.ArrayList;
import java.util.List;

public class AddressAutocompleteAdapter extends ArrayAdapter<String> implements Filterable {
    private final LayoutInflater mInflater;

    public AddressAutocompleteAdapter(final Context context) {
        super(context, -1);
        mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(final int position, final View view, @NonNull final ViewGroup parentView) {
        final TextView newView;
        if (view == null) {
            newView = (TextView) mInflater.inflate(android.R.layout.simple_dropdown_item_1line, parentView, false);
        } else {
            newView = (TextView) view;
        }
        newView.setText(getItem(position));
        return newView;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return (new Filter() {
            private List<String> convertObjectToStringList(FilterResults filterResults) {
                RuntimeAssertion.require(filterResults.values instanceof List<?>);
                ArrayList<String> out = new ArrayList<>();
                for (Object suggestion : (List<?>) filterResults.values) {
                    if (suggestion instanceof String) {
                        out.add((String) suggestion);
                    }
                }
                return out;
            }

            @Override
            protected void publishResults(final CharSequence constraint, final FilterResults filterResults) {
                final List<String> suggestions = convertObjectToStringList(filterResults);
                clear();
                for (String suggestion : suggestions) {
                    add(suggestion);
                }
                notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(final CharSequence constraint) {
                final ArrayList<String> suggestions = new ArrayList<>();

                if (constraint != null) {
                    // case insensitive search: address strings are always lower case
                    final String constraintString = constraint.toString().toLowerCase();

                    if (SessionConnector.get().sessionAvailable()) {
                        List<String> known = SessionConnector.get().getKnownAddressesAsString();
                        for (String address : known) {
                            if (address.startsWith(constraintString)) {
                                suggestions.add(address);
                            }
                        }
                    }
                }

                final FilterResults filterResults = new FilterResults();
                filterResults.values = suggestions;
                filterResults.count = suggestions.size();

                return filterResults;
            }
        });
    }
}
