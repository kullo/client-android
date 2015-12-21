/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.conversationslist;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration;

import net.kullo.android.R;
import net.kullo.android.kulloapi.DialogMaker;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.kulloapi.KulloUtils;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.android.observers.eventobservers.ConversationsEventObserver;
import net.kullo.android.observers.listenerobservers.SyncerListenerObserver;
import net.kullo.android.screens.MessagesListActivity;
import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.NetworkError;
import net.kullo.libkullo.api.SyncProgress;

import java.util.List;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class ConversationsFragment extends Fragment {
    public static final String TAG = "ConversationsFragment";

    protected RecyclerView mRecyclerView;
    private ConversationsAdapter mAdapter;
    private View mRootView;
    private SwipeRefreshLayout mSwipeLayout;
    private MaterialProgressBar mProgressBar;
    private boolean mIsPaused;
    private ConversationsEventObserver mConversationsEventObserver;
    private SyncerListenerObserver mSyncerListenerObserver;
    private ActionMode mActionMode = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // has it's own actionbar items
        setHasOptionsMenu(true);
    }

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_conversations, container, false);
        RuntimeAssertion.require(mRootView != null);

        mRootView.setTag(TAG);

        setupConversationsList();
        setupSwipeRefreshLayout();

        return mRootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        mConversationsEventObserver = new ConversationsEventObserver() {
            @Override
            public void conversationAdded(long conversationId) {
                Log.d(TAG, "Got a new conversation. Refresh view!");
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        reloadConversationsList();
                    }
                });
            }

            @Override
            public void conversationChanged(long conversationId) {
                Log.d(TAG, "Conversation changed. Refresh View!");
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        reloadConversationsList();
                    }
                });
            }

            @Override
            public void conversationRemoved(long conversationId) {
                Log.d(TAG, "Conversation deleted. Refresh view!");
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        reloadConversationsList();
                    }
                });
            }
        };
        SessionConnector.get().addEventObserver(
                ConversationsEventObserver.class,
                mConversationsEventObserver);

        registerSyncListenerObserver();
    }

    @Override
    public void onStop() {
        super.onStop();

        unregisterSyncFinishedListenerObserver();

        SessionConnector.get().removeEventObserver(
                ConversationsEventObserver.class,
                mConversationsEventObserver);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        RuntimeAssertion.require(SessionConnector.get().sessionAvailable());

        // set title
        getActivity().setTitle(R.string.menu_conversations);
    }

    @Override
    public void onPause() {
        super.onPause();

        // make sure that selection is clear if we leave this view
        clearSelection();

        mIsPaused = true;

        // TODO: Cancel sync
    }

    @Override
    public void onResume(){
        super.onResume();

        mIsPaused = false;

        if (SessionConnector.get().isSyncing()) {
            mSwipeLayout.setEnabled(false);
            mSwipeLayout.setRefreshing(true);
        } else {
            mSwipeLayout.setEnabled(true);
            mSwipeLayout.setRefreshing(false);
            mProgressBar.setVisibility(View.GONE);
        }

        reloadConversationsList();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.appbar_menu_conversations_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection, if it doesn't land here, the activity might have snatched it
        switch (item.getItemId()) {
            case R.id.action_refresh:
                mSwipeLayout.setEnabled(false);
                mSwipeLayout.setRefreshing(true);
                SessionConnector.get().syncKullo();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupSwipeRefreshLayout() {
        mProgressBar = (MaterialProgressBar) mRootView.findViewById(R.id.horizontal_progress_toolbar);

        mSwipeLayout = (SwipeRefreshLayout) mRootView.findViewById(R.id.swipe_refresh_layout);
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeLayout.setEnabled(false); // Lock it
                SessionConnector.get().syncKullo();
            }
        });
    }

    private void registerSyncListenerObserver() {
        // avoid any unnecessary work when fragment's activity is paused (mIsPaused == true)
        mSyncerListenerObserver = new SyncerListenerObserver() {
            @Override
            public void draftAttachmentsTooBig(long convId) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mSwipeLayout != null) {
                            mSwipeLayout.setEnabled(true);
                            mSwipeLayout.setRefreshing(false);
                        }

                        //TODO: dialog with the option to clear the attachments of the drafts --> comes later with attachments
                    }
                });
            }

            @Override
            public void progressed(final SyncProgress progress) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (KulloUtils.showSyncProgressAsBar(progress)) {
                            int percent = Math.round(100 * ((float) progress.getCountProcessed() / progress.getCountTotal()));

                            mSwipeLayout.setEnabled(false);
                            mSwipeLayout.setRefreshing(false);
                            mProgressBar.setVisibility(View.VISIBLE);

                            mProgressBar.setProgress(percent);
                        } else {
                            mSwipeLayout.setEnabled(false);
                            mSwipeLayout.setRefreshing(true);
                        }
                    }
                });
            }

            @Override
            public void finished() {
                Log.d(TAG, "Update conversations view after sync ...");

                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mSwipeLayout != null) {
                                mSwipeLayout.setEnabled(true);
                                mSwipeLayout.setRefreshing(false);
                            }

                            mProgressBar.setVisibility(View.GONE);

                            if (!mIsPaused) reloadConversationsList();
                        }
                    });
                } else {
                    Log.e(TAG, "Cannot get Activity.");
                }
            }

            @Override
            public void error(final NetworkError error) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mSwipeLayout != null) {
                            mSwipeLayout.setEnabled(true);
                            mSwipeLayout.setRefreshing(false);
                        }

                        Toast.makeText(getActivity(),
                                DialogMaker.getTextForNetworkError(getActivity(), error),
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        };
        SessionConnector.get().addListenerObserver(
                SyncerListenerObserver.class,
                mSyncerListenerObserver);
    }

    private void unregisterSyncFinishedListenerObserver() {
        SessionConnector.get().removeListenerObserver(
                SyncerListenerObserver.class,
                mSyncerListenerObserver);
    }

    private void setupConversationsList() {
        //set up recycler view
        mRecyclerView = (RecyclerView) mRootView.findViewById(R.id.conversations_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mAdapter = new ConversationsAdapter(this.getActivity());
        mRecyclerView.setAdapter(mAdapter);

        // Add the sticky headers decoration
        final StickyRecyclerHeadersDecoration headersDecor = new StickyRecyclerHeadersDecoration(mAdapter);
        mRecyclerView.addItemDecoration(headersDecor);

        // Add decoration for dividers between list items
        int dividerLeftMargin = getActivity().getResources().getDimensionPixelSize(R.dimen.md_additions_list_divider_margin_left);
        mRecyclerView.addItemDecoration(new DividerDecoration(getActivity(), dividerLeftMargin));

        // Add touch listener
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), mRecyclerView,
            new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Long conversationId = mAdapter.getItem(position);

                if (mAdapter.isSelectionActive()) {
                    selectConversation(view, conversationId);
                } else {
                    Intent intent = new Intent(getActivity(), MessagesListActivity.class);
                    intent.putExtra(MessagesListActivity.CONVERSATION_ID, conversationId);

                    startActivity(intent);
                }
            }
            @Override
            public void onItemLongPress(View view, int position) {
                Long conversationId = mAdapter.getItem(position);
                selectConversation(view, conversationId);
            }
        }));
    }

    private void setVisibilityControlsIfListIsEmpty() {
        ImageView swipeToRefreshImage = (ImageView) mRootView.findViewById(R.id.swipe_to_refresh_image);
        FloatingActionButton fab = (FloatingActionButton) mRootView.findViewById(R.id.fab_conversations);
        TextView swipeToRefreshText = (TextView) mRootView.findViewById(R.id.swipe_to_refresh_text);

        if (mAdapter.getItemCount() == 0) {
            swipeToRefreshImage.setVisibility(View.VISIBLE);
            swipeToRefreshText.setVisibility(View.VISIBLE);
            fab.setVisibility(View.GONE);
        } else {
            swipeToRefreshImage.setVisibility(View.GONE);
            swipeToRefreshText.setVisibility(View.GONE);
            fab.setVisibility(View.VISIBLE);
        }
    }

    private void reloadConversationsList() {
        RuntimeAssertion.require(mAdapter != null);

        // When list is reloaded, selection is not preserved.
        // So stop action mode as well
        clearSelection();

        List<Long> conversationIds = SessionConnector.get().getAllConversationIdsSorted();
        mAdapter.replaceAll(conversationIds);

        setVisibilityControlsIfListIsEmpty();
    }

    private void selectConversation(View view, final Long conversationId) {
        // (de)select conversation
        mAdapter.toggleSelectedItem(conversationId);
        if (!mAdapter.isSelectionActive()) {
            mActionMode.finish();
            return;
        }

        if (mActionMode == null) {
            // start new action mode
            mActionMode = getActivity().startActionMode(new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    MenuInflater inflater = mode.getMenuInflater();
                    inflater.inflate(R.menu.appbar_menu_message_actions, menu);
                    return true;
                }
                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    Ui.setColorStatusBarArrangeHeader(getActivity());
                    return true;
                }
                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.action_delete: {
                            // only delete empty conversations
                            int skippedConversationsCount = 0;
                            for (Long convId : mAdapter.getSelectedItems()) {
                                if (SessionConnector.get().getMessageCount(convId) == 0) {
                                    SessionConnector.get().removeConversation(convId);
                                } else {
                                    skippedConversationsCount++;
                                }
                            }
                            if (skippedConversationsCount > 0) {
                                final String text = String.format(
                                        getActivity().getResources().getString(R.string.toast_n_skipped),
                                        skippedConversationsCount);
                                Toast.makeText(getActivity().getApplicationContext(), text, Toast.LENGTH_LONG).show();
                            }
                            mode.finish();
                            return true;
                        }
                        default:
                            return false;
                    }
                }
                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    mAdapter.clearSelectedItems();
                    mActionMode = null;
                }
            });
        }

        // update action bar title
        final String title = String.format(
                getActivity().getResources().getString(R.string.title_n_selected),
                mAdapter.getSelectedItemsCount());
        mActionMode.setTitle(title);
    }

    public void clearSelection() {
        if (mActionMode != null) {
            mAdapter.clearSelectedItems();
            mActionMode.finish();
        }
    }
}
