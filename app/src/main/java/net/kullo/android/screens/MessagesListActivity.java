/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.screens;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import net.kullo.android.R;
import net.kullo.android.application.CommonDialogs;
import net.kullo.android.application.KulloActivity;
import net.kullo.android.kulloapi.CreateSessionResult;
import net.kullo.android.kulloapi.CreateSessionState;
import net.kullo.android.kulloapi.DialogMaker;
import net.kullo.android.kulloapi.KulloUtils;
import net.kullo.android.kulloapi.MessagesComparatorDsc;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.android.notifications.GcmConnector;
import net.kullo.android.observers.eventobservers.MessageAddedEventObserver;
import net.kullo.android.observers.eventobservers.MessageRemovedEventObserver;
import net.kullo.android.observers.eventobservers.MessageStateEventObserver;
import net.kullo.android.observers.listenerobservers.SyncerListenerObserver;
import net.kullo.android.screens.messageslist.ConversationAdapter;
import net.kullo.android.screens.messageslist.MessagesAdapter;
import net.kullo.android.ui.DividerDecoration;
import net.kullo.android.ui.RecyclerItemClickListener;
import net.kullo.android.util.ItemVisibilityObserver;
import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.NetworkError;
import net.kullo.libkullo.api.SyncProgress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import io.github.dialogsforandroid.MaterialDialog;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class MessagesListActivity extends KulloActivity {
    public static final String TAG = "MessagesListActivity";

    private MessagesAdapter mMessagesAdapter;
    private Long mConversationId;
    private MessageAddedEventObserver mMessageAddedObserver;
    private MessageRemovedEventObserver mMessageDeletedObserver;
    private MessageStateEventObserver mMessageStateObserver;

    private SyncerListenerObserver mSyncerListenerObserver;
    @Nullable private ActionMode mActionMode = null;

    // Views
    private SwipeRefreshLayout mSwipeLayout;
    private MaterialProgressBar mProgressBarDeterminate;
    private MaterialProgressBar mProgressBarIndeterminate;
    private RecyclerView mMessagesList;
    private LinearLayoutManager mLayoutManager;
    private TextView mConversationEmptyLabel;

    // when requested, scroll to top will be performed at the end of onResume()
    private boolean mScrollToTopRequested;
    private static int MESSAGE_VISIBILITY_BEFORE_READ_MS = 2000;
    private ItemVisibilityObserver<Long> mVisibleMessagesObserver = new ItemVisibilityObserver<>(MESSAGE_VISIBILITY_BEFORE_READ_MS);
    private Timer mVisibleItemsObserverTimer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final CreateSessionResult result = SessionConnector.get().createActivityWithSession(this);
        if (result.state == CreateSessionState.NO_CREDENTIALS) return;

        setContentView(R.layout.activity_messages_list);

        Intent intent = getIntent();
        mConversationId = intent.getLongExtra(KulloConstants.CONVERSATION_ID, -1);

        Ui.prepareActivityForTaskManager(this);
        Ui.setStatusBarColor(this, false, Ui.LayoutType.CoordinatorLayout);
        Ui.setupActionbar(this);

        setupMessagesList();
        setupVisibleMessagesObserver();

        mProgressBarDeterminate = (MaterialProgressBar) findViewById(R.id.progressbar_determinate);
        mProgressBarIndeterminate = (MaterialProgressBar) findViewById(R.id.progressbar_indeterminate);

        mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                SessionConnector.get().sync();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // progress bar will take over, so we don't need this rotating
                        mSwipeLayout.setRefreshing(false);
                    }
                }, 500);
            }
        });

        if (result.state == CreateSessionState.CREATING) {
            RuntimeAssertion.require(result.task != null);
            result.task.waitUntilDone();
        }

        GcmConnector.get().ensureSessionHasTokenRegisteredAsync();
    }

    @Override
    protected void onStart() {
        super.onStart();

        setTitle(SessionConnector.get().getConversationNameOrPlaceHolder(mConversationId));

        mMessageAddedObserver = new MessageAddedEventObserver() {
            @Override
            public void messageAdded(final long conversationId, final long messageId) {
                if (conversationId == mConversationId) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMessagesAdapter.add(messageId, new MessagesComparatorDsc());

                            if (SessionConnector.get().messageIncoming(messageId)) {
                                scrollToTopAnimated();
                            }
                        }
                    });
                }
            }
        };
        SessionConnector.get().addEventObserver(
                MessageAddedEventObserver.class,
                mMessageAddedObserver);

        mMessageDeletedObserver = new MessageRemovedEventObserver() {
            @Override
            public void messageRemoved(final long conversationId, final long messageId) {
                if (conversationId == mConversationId) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMessagesAdapter.remove(messageId);
                            Log.d(TAG, "Message item removed (id: " + messageId + ").");
                        }
                    });
                }
            }
        };
        SessionConnector.get().addEventObserver(
                MessageRemovedEventObserver.class,
                mMessageDeletedObserver);

        mMessageStateObserver = new MessageStateEventObserver() {
            @Override
            public void messageStateChanged(final long conversationId, final long messageId) {
                if (conversationId == mConversationId) {
                    boolean handled = mMessagesAdapter.tryHandleElementChanged(messageId, new MessagesComparatorDsc());
                    RuntimeAssertion.require(handled, "Message positions do not change, so this must be true");
                    Log.d(TAG, "Message item changed (id: " + messageId + ").");
                }
            }
        };
        SessionConnector.get().addEventObserver(
                MessageStateEventObserver.class,
                mMessageStateObserver);


        registerSyncFinishedListenerObserver();
        SessionConnector.get().syncIfNecessary();

        // Fill adapter with the most recent data
        RuntimeAssertion.require(mMessagesAdapter != null);
        mMessagesAdapter.replaceAll(SessionConnector.get().getAllMessageIdsSorted(mConversationId));
    }

    // Called immediately before onResume()
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case KulloConstants.REQUEST_CODE_NEW_MESSAGE:
                mScrollToTopRequested = true;
                break;
            default:
                Log.w(TAG, "Unhandled request code.");
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        GcmConnector.get().removeAllNotifications(this);

        mVisibleMessagesObserver.resetTimes();

        mVisibleItemsObserverTimer = new Timer(true);
        mVisibleItemsObserverTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Set<Long> visibleItems = getFullyVisibleMessages();
                        Log.d(TAG, "Visible: " + visibleItems);
                        mVisibleMessagesObserver.resetTimesExcept(visibleItems);

                        Set<Long> readyItems = mVisibleMessagesObserver.getReadyItems();
                        Log.d(TAG, "Ready: " + readyItems);
                        SessionConnector.get().setMessagesRead(readyItems);
                    }
                });
            }
        }, MESSAGE_VISIBILITY_BEFORE_READ_MS / 5, MESSAGE_VISIBILITY_BEFORE_READ_MS / 5);

        if (SessionConnector.get().isSyncing()) {
            updateSwipeLayout(true);

            mProgressBarIndeterminate.setVisibility(View.VISIBLE);
            mProgressBarDeterminate.setVisibility(View.GONE);
        } else {
            updateSwipeLayout(false);

            mProgressBarIndeterminate.setVisibility(View.GONE);
            mProgressBarDeterminate.setVisibility(View.GONE);
        }

        if (mScrollToTopRequested) {
            scrollToTopAnimated(500);
            mScrollToTopRequested = false;
        }
    }

    @UiThread
    private Set<Long> getFullyVisibleMessages() {
        final int ROWS_BEFORE_MESSAGES = 1;

        int posInConversationFirst = mLayoutManager.findFirstCompletelyVisibleItemPosition();
        int posInConversationLast = mLayoutManager.findLastCompletelyVisibleItemPosition();

        if (posInConversationFirst == RecyclerView.NO_POSITION || posInConversationLast == RecyclerView.NO_POSITION) {
            return Collections.emptySet();
        } else if (mMessagesAdapter.getItemCount() == 0) {
            return Collections.emptySet();
        } else {
            // all positions given in messages adapter indices
            int posFirst = posInConversationFirst - ROWS_BEFORE_MESSAGES;
            int posLast = posInConversationLast - ROWS_BEFORE_MESSAGES;

            // when rows before messages are visible, we need to push the starting point down
            if (posFirst < 0) {
                posFirst = 0;
            }

            Set<Long> visibleItems = new HashSet<>();
            for (int pos = posFirst; pos <= posLast; ++pos) {
                visibleItems.add(mMessagesAdapter.getItem(pos));
            }
            return visibleItems;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mVisibleItemsObserverTimer.cancel();
        mVisibleItemsObserverTimer = null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        SessionConnector.get().removeEventObserver(
                MessageAddedEventObserver.class,
                mMessageAddedObserver);
        SessionConnector.get().removeEventObserver(
                MessageRemovedEventObserver.class,
                mMessageDeletedObserver);
        SessionConnector.get().removeEventObserver(
                MessageStateEventObserver.class,
                mMessageStateObserver);

        unregisterSyncFinishedListenerObserver();
    }

    @UiThread
    private void setupVisibleMessagesObserver() {

        mMessagesAdapter.setFullyVisibleCallback(new MessagesAdapter.FullyVisibleCallback() {
            @Override
            public void onUnreadAndFullyVisible(Long item) {
                mVisibleMessagesObserver.add(item);
            }

            @Override
            public void onNotUnreadAndFullyVisible(Long item) {
                mVisibleMessagesObserver.remove(item);
            }
        });
    }

    @UiThread
    private void scrollToTopAnimated() {
        scrollToTopAnimated(0);
    }

    @UiThread
    private void scrollToTopAnimated(int delayMs) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mMessagesList.smoothScrollToPosition(0);
            }
        }, delayMs);
    }

    private void updateSwipeLayout(boolean isSyncing) {
        mSwipeLayout.setEnabled(!isSyncing);
    }

    private void setupMessagesList() {
        CoordinatorLayout mainLayout = (CoordinatorLayout) findViewById(R.id.main_layout);
        Ui.setStatusBarColor(mainLayout);

        mMessagesList = (RecyclerView) findViewById(R.id.messagesList);
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mMessagesList.setLayoutManager(mLayoutManager);

        // Add decoration for dividers between list items
        int dividerLeftMargin = getResources().getDimensionPixelSize(R.dimen.md_additions_list_divider_margin_left);
        mMessagesList.addItemDecoration(new DividerDecoration(this, dividerLeftMargin));

        mMessagesAdapter = new MessagesAdapter(this);
        mMessagesAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() { // Initial loading of data
                updateEmptyLabel();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                updateEmptyLabel();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                updateEmptyLabel();
            }
        });
        mMessagesList.setAdapter(new ConversationAdapter(mMessagesAdapter, mConversationId));

        mMessagesList.addOnItemTouchListener(new RecyclerItemClickListener(mMessagesList) {

            @SuppressWarnings("StatementWithEmptyBody")
            @Override
            public void onItemClick(View view, int position) {
                if (position == 0) {
                    if (!mMessagesAdapter.isSelectionActive()) {
                        Intent intent = new Intent(MessagesListActivity.this, ConversationInfoActivity.class);
                        intent.putExtra(KulloConstants.CONVERSATION_ID, mConversationId);
                        startActivity(intent);
                    } else {
                        // ignore tap on conversation info when action mode is active
                    }
                } else {
                    int messagesAdapterPosition = position - 1;
                    long messageId = mMessagesAdapter.getItem(messagesAdapterPosition);

                    if (mMessagesAdapter.isSelectionActive()) {
                        messageSelected(messageId);
                    } else {
                        Intent intent = new Intent(MessagesListActivity.this, SingleMessageActivity.class);
                        intent.putExtra(KulloConstants.MESSAGE_ID, messageId);
                        startActivity(intent);
                    }
                }
            }

            @SuppressWarnings("StatementWithEmptyBody")
            @Override
            public void onItemLongPress(View view, int position) {
                if (position == 0) {
                    // do nothing
                } else {
                    int messagesAdapterPosition = position - 1;
                    long messageId = mMessagesAdapter.getItem(messagesAdapterPosition);
                    messageSelected(messageId);
                }
            }
        });

        mConversationEmptyLabel = (TextView) findViewById(R.id.empty_list_label);
    }

    private void updateEmptyLabel() {
        if (mMessagesAdapter != null) {
            if (mMessagesAdapter.getItemCount() > 0) {
                mConversationEmptyLabel.setVisibility(View.GONE);
                mMessagesList.setVisibility(View.VISIBLE);
            } else {
                mConversationEmptyLabel.setVisibility(View.VISIBLE);
                mMessagesList.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                SessionConnector.get().sync();
                return true;
            case R.id.action_toggle_message_size:
                toggleMessageSize();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.appbar_menu_messages_list, menu);
        setupSearchAction(menu.findItem(R.id.action_search), mConversationId);

        if (mMessagesAdapter.isShowCardsExpanded()) {
            menu.findItem(R.id.action_toggle_message_size).setIcon(R.drawable.kullo_unfold_less);
        } else {
            menu.findItem(R.id.action_toggle_message_size).setIcon(R.drawable.kullo_unfold_more);
        }

        return true;
    }

    private void toggleMessageSize () {
        mMessagesAdapter.toggleMessageSize();
        supportInvalidateOptionsMenu();
    }

    public void replyClicked(View v) {
        if (SessionConnector.get().userSettingsAreValidForSync()) {
            Intent intent = new Intent(this, ComposeActivity.class);
            intent.putExtra(KulloConstants.CONVERSATION_ID, mConversationId);
            startActivityForResult(intent, KulloConstants.REQUEST_CODE_NEW_MESSAGE);
        } else {
            showDialogToShowUserSettingsForCompletion();
        }
    }

    private void showDialogToShowUserSettingsForCompletion() {
        final MaterialDialog showSettingsDialog = CommonDialogs.buildShowSettingsDialog(this);
        showSettingsDialog.show();
    }

    private void registerSyncFinishedListenerObserver() {
        // avoid any unnecessary work when fragment's activity is paused (mIsPaused == true)
        mSyncerListenerObserver = new SyncerListenerObserver() {
            @Override
            public void started() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateSwipeLayout(true);

                        mProgressBarDeterminate.setVisibility(View.GONE);
                        mProgressBarIndeterminate.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void draftAttachmentsTooBig(long convId) {
            }

            @Override
            public void progressed(final SyncProgress progress) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (KulloUtils.showSyncProgressAsBar(progress)) {
                            int percent = Math.round(100 * ((float) progress.getIncomingMessagesProcessed() / progress.getIncomingMessagesTotal()));
                            mProgressBarIndeterminate.setVisibility(View.GONE);
                            mProgressBarDeterminate.setVisibility(View.VISIBLE);
                            mProgressBarDeterminate.setProgress(percent);
                        } else {
                            mProgressBarIndeterminate.setVisibility(View.VISIBLE);
                            mProgressBarDeterminate.setVisibility(View.GONE);
                        }
                    }
                });
            }

            @Override
            public void finished() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateSwipeLayout(false);

                        mProgressBarDeterminate.setVisibility(View.GONE);
                        mProgressBarIndeterminate.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void error(final NetworkError error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateSwipeLayout(false);

                        mProgressBarDeterminate.setVisibility(View.GONE);
                        mProgressBarIndeterminate.setVisibility(View.GONE);

                        Toast.makeText(MessagesListActivity.this,
                                DialogMaker.getTextForNetworkError(MessagesListActivity.this, error),
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

    private void messageSelected(final long messageId) {
        mMessagesAdapter.toggleSelectedItem(messageId);
        if (!mMessagesAdapter.isSelectionActive()) {
            if (mActionMode != null) mActionMode.finish();
            return;
        }

        if (mActionMode == null) setupActionMode();

        final String title = String.format(
                getResources().getString(R.string.actionmode_title_n_selected),
                mMessagesAdapter.getSelectedItemsCount());
        mActionMode.setTitle(title);
    }

    private void setupActionMode() {
        mActionMode = startActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.appbar_menu_message_actions, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                Ui.setStatusBarColor(MessagesListActivity.this, true, Ui.LayoutType.CoordinatorLayout);
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_delete:
                        List<Long> selectedMessageIdsCopy = new ArrayList<>(mMessagesAdapter.getSelectedItems());
                        for (long selectedMessageId : selectedMessageIdsCopy) {
                            SessionConnector.get().removeMessage(selectedMessageId);
                        }
                        SessionConnector.get().sync();

                        mode.finish();
                        return true;
                    default:
                        return false;
                }
            }
            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mMessagesAdapter.clearSelectedItems();
                Ui.setStatusBarColor(MessagesListActivity.this, false, Ui.LayoutType.CoordinatorLayout);
                mActionMode = null;
            }
        });
    }
}
