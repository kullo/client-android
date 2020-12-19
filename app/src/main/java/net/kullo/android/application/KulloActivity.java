/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.application;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.MenuItem;
import android.widget.LinearLayout;

import net.kullo.android.R;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.screens.SearchActivity;
import net.kullo.android.ui.LayoutHelpers;

public class KulloActivity extends AppCompatActivity {
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void setupSearchAction(MenuItem menuItem, final long conversationId) {
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Intent searchIntent = new Intent(KulloActivity.this, SearchActivity.class);
                searchIntent.setAction(Intent.ACTION_SEARCH);
                searchIntent.putExtra(SearchManager.QUERY, query);

                Bundle appData = new Bundle();
                appData.putLong(KulloConstants.CONVERSATION_ID, conversationId);
                searchIntent.putExtra(SearchManager.APP_DATA, appData);

                startActivity(searchIntent);

                return true; // i.e. query handled by the listener
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false; // i.e. perform default action
            }
        });

        // Adjust layout of support library's abc_search_view.xml
        LinearLayout searchEditFrame = (LinearLayout) searchView.findViewById(R.id.search_edit_frame);
        LayoutHelpers.setLeftMargin(searchEditFrame, 0);
        LayoutHelpers.setRightMargin(searchEditFrame, 0);
    }
}
