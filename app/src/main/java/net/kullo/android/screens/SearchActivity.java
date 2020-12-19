/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.screens;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import net.kullo.android.R;
import net.kullo.android.application.KulloActivity;
import net.kullo.android.kulloapi.CreateSessionResult;
import net.kullo.android.kulloapi.CreateSessionState;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.android.notifications.GcmConnector;
import net.kullo.android.screens.search.SearchAdapter;
import net.kullo.android.screens.search.SearchResultsAdapter;
import net.kullo.android.ui.DividerDecoration;
import net.kullo.android.util.adapters.RecyclerItemClickListener;
import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.MessagesSearchResult;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SearchActivity extends KulloActivity {
    private static final String TAG = SearchActivity.class.getSimpleName();

    @NonNull private String mQuery = "";
    @NonNull private SessionConnector.MessageDirection mDirection = SessionConnector.MessageDirection.ALL;
    private long mConversationId = -1;

    @NonNull private SearchResultsAdapter mResultsAdapter = new SearchResultsAdapter();

    @BindView(R.id.results_list) RecyclerView resultsList;
    @BindView(R.id.results_empty) TextView resultsEmpty;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final CreateSessionResult result = SessionConnector.get().createActivityWithSession(this);
        if (result.state == CreateSessionState.NO_CREDENTIALS) return;

        setContentView(R.layout.activity_search);

        Ui.prepareActivityForTaskManager(this);
        Ui.setupActionbar(this);
        Ui.setStatusBarColor(this);
        ButterKnife.bind(this);

        setTitle(R.string.search_title);

        resultsList.setAdapter(new SearchAdapter(mResultsAdapter));
        resultsList.setLayoutManager(new LinearLayoutManager(this));

        mResultsAdapter.setOnClickListener(new RecyclerItemClickListener() {
            @Override
            public void onClick(View v, int position) {
                int resultsPosition = position - 1;
                RuntimeAssertion.require(resultsPosition >= 0);

                long messageId = mResultsAdapter.getItem(resultsPosition).getMsgId();
                Intent intent = new Intent(SearchActivity.this, SingleMessageActivity.class);
                intent.putExtra(KulloConstants.MESSAGE_ID, messageId);
                startActivity(intent);
            }

            @Override
            public boolean onLongClick(View v, int position) {
                return false;
            }
        });

        // Add decoration for dividers between list items
        int dividerLeftMargin = getResources().getDimensionPixelSize(R.dimen.md_additions_list_divider_margin_left);
        resultsList.addItemDecoration(new DividerDecoration(this, dividerLeftMargin));

        if (result.state == CreateSessionState.CREATING) {
            RuntimeAssertion.require(result.task != null);
            result.task.waitUntilDone();
        }

        GcmConnector.get().ensureSessionHasTokenRegisteredAsync();

        handleSearchIntent();
    }

    private void handleSearchIntent() {
        Intent intent = getIntent();

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            Bundle bundle = intent.getBundleExtra(SearchManager.APP_DATA);
            RuntimeAssertion.require(bundle != null);
            mConversationId = bundle.getLong(KulloConstants.CONVERSATION_ID, -1);

            String query = intent.getStringExtra(SearchManager.QUERY);
            mQuery = query.trim();

            Log.d(TAG, "Searching for '"  + mQuery + "' in " + mConversationId);
            updateSearchResultsAsync();

            setTitle(getString(R.string.search_title) + ": " + mQuery);
        }
    }

    private void updateSearchResultsAsync() {
        // copy query to have correct value in results callback
        final String query = mQuery;

        SessionConnector.get().search(query, mDirection, mConversationId, new SessionConnector.SearchCallback() {
            @Override
            @MainThread
            public void finished(final ArrayList<MessagesSearchResult> results) {
                mResultsAdapter.reset(results);

                if (results.isEmpty()) {
                    resultsEmpty.setText(String.format(getString(R.string.search_no_results), query));
                    resultsEmpty.setVisibility(View.VISIBLE);
                } else {
                    resultsEmpty.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                final Intent upIntent;
                if (mConversationId != -1) {
                    upIntent = new Intent(this, MessagesListActivity.class);
                    upIntent.putExtra(KulloConstants.CONVERSATION_ID, mConversationId);
                } else {
                    upIntent = new Intent(this, ConversationsListActivity.class);
                }
                NavUtils.navigateUpTo(this, upIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onMessageDirectionChanged(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        RuntimeAssertion.require(checked);

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.direction_all:
                mDirection = SessionConnector.MessageDirection.ALL;
                break;
            case R.id.direction_received:
                mDirection = SessionConnector.MessageDirection.INCOMING;
                break;
            case R.id.direction_sent:
                mDirection = SessionConnector.MessageDirection.OUTGOING;
                break;
        }

        updateSearchResultsAsync();
    }
}
