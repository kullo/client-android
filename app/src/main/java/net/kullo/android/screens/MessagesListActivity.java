/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import net.kullo.android.R;
import net.kullo.android.kulloapi.KulloConnector;
import net.kullo.android.kulloapi.MessagesComparatorDsc;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.android.observers.eventobservers.MessageAddedEventObserver;
import net.kullo.android.observers.eventobservers.MessageRemovedEventObserver;
import net.kullo.android.observers.eventobservers.MessageStateEventObserver;
import net.kullo.android.observers.listenerobservers.DownloadAttachmentsForMessageListenerObserver;
import net.kullo.android.observers.listenerobservers.SyncerRunListenerObserver;
import net.kullo.android.screens.conversationslist.DividerDecoration;
import net.kullo.android.screens.conversationslist.RecyclerItemClickListener;
import net.kullo.android.screens.messageslist.MessageAttachmentsOpener;
import net.kullo.android.screens.messageslist.MessagesAdapter;
import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.AsyncTask;

import java.util.ArrayList;
import java.util.List;

public class MessagesListActivity extends AppCompatActivity {
    public static final String TAG = "MessagesListActivity";
    public static final String CONVERSATION_ID = "conversation_id";

    private static final boolean SHOW_AVATAR_ROW = false;
    private MessagesAdapter mMessagesAdapter;
    private Long mConversationId;
    private MessageAttachmentsOpener mMessageAttachmentsOpener;
    private MessageAddedEventObserver mMessageAddedObserver;
    private MessageRemovedEventObserver mMessageDeletedObserver;
    private MessageStateEventObserver mMessageStateObserver;
    private DownloadAttachmentsForMessageListenerObserver mDownloadAttachmentsFinishedObserver;

    private SyncerRunListenerObserver mSyncerRunListenerObserver;
    private ActionMode mActionMode = null;
    // Views
    private SwipeRefreshLayout mSwipeLayout;
    private RecyclerView mMessagesList;
    private MaterialDialog mShowSettingsDialog;
    private TextView mConversationEmptyLabel;

    private LinearLayout mAvatarsRow;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AsyncTask task = KulloConnector.get().createActivityWithSession(this);

        setContentView(R.layout.activity_messages_list);

        Intent intent = getIntent();
        mConversationId = intent.getLongExtra(CONVERSATION_ID, -1);

        Ui.setupActionbar(this);
        Ui.setColorStatusBarArrangeHeader(this);

        mDownloadAttachmentsFinishedObserver = new DownloadAttachmentsForMessageListenerObserver() {
            @Override
            public void finished() {
            }

            @Override
            public void error(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new MaterialDialog.Builder(MessagesListActivity.this)
                                .title(R.string.error_title)
                                .content(error)
                                .neutralText(R.string.ok)
                                .cancelable(false)
                                .show();
                    }
                });
            }
        };
        KulloConnector.get().addListenerObserver(
                DownloadAttachmentsForMessageListenerObserver.class,
                mDownloadAttachmentsFinishedObserver);

        setupMessagesList();

        mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeLayout.setEnabled(false); // Lock it
                KulloConnector.get().syncKullo();
            }
        });

        if (task != null) task.waitUntilDone();
    }

    @Override
    protected void onStart() {
        super.onStart();

        setTitle(KulloConnector.get().getConversationNameOrPlaceHolder(mConversationId));

        updateSenderAvatarViewsInHeader();

        mMessageAddedObserver = new MessageAddedEventObserver() {
            @Override
            public void messageAdded(final long conversationId, final long messageId) {
                if (conversationId == mConversationId) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesComparatorDsc comparator = new MessagesComparatorDsc(KulloConnector.get().getSession());
                            mMessagesAdapter.add(messageId, comparator);
                            Log.d(TAG, "Message item added (id: " + messageId + "). " + comparator.getStats());
                        }
                    });
                }
            }
        };
        KulloConnector.get().addEventObserver(
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
        KulloConnector.get().addEventObserver(
                MessageRemovedEventObserver.class,
                mMessageDeletedObserver);

        mMessageStateObserver = new MessageStateEventObserver() {
            @Override
            public void messageStateChanged(final long conversationId, final long messageId) {
                if (conversationId == mConversationId) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMessagesAdapter.notifyDataForIdChanged(messageId);
                            Log.d(TAG, "Message item changed (id: " + messageId + ").");
                        }
                    });
                }
            }
        };
        KulloConnector.get().addEventObserver(
                MessageStateEventObserver.class,
                mMessageStateObserver);


        registerSyncFinishedListenerObserver();
    }

    @Override
    protected void onStop() {
        super.onStop();
        KulloConnector.get().removeEventObserver(
                MessageAddedEventObserver.class,
                mMessageAddedObserver);
        KulloConnector.get().removeEventObserver(
                MessageRemovedEventObserver.class,
                mMessageDeletedObserver);
        KulloConnector.get().removeEventObserver(
                MessageStateEventObserver.class,
                mMessageStateObserver);

        unregisterSyncFinishedListenerObserver();
    }

    private void setupMessagesList() {
        mMessagesList = (RecyclerView) findViewById(R.id.messagesList);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mMessagesList.setLayoutManager(llm);

        // Add decoration for dividers between list items
        int dividerLeftMargin = getResources().getDimensionPixelSize(R.dimen.md_additions_list_divider_margin_left);
        mMessagesList.addItemDecoration(new DividerDecoration(this, dividerLeftMargin));

        mMessageAttachmentsOpener = new MessageAttachmentsOpener(this);
        mMessageAttachmentsOpener.registerSaveFinishedListenerObserver();
        mMessagesAdapter = new MessagesAdapter(this, mConversationId, mMessageAttachmentsOpener);
        mMessagesAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                // called on addition of elements
                updateEmptyLabel();
            }
            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                // called on removal of elements
                updateEmptyLabel();
            }
        });
        mMessagesList.setAdapter(mMessagesAdapter);

        // Add touch listener
        mMessagesList.addOnItemTouchListener(new RecyclerItemClickListener(MessagesListActivity.this, mMessagesList,
            new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Long messageId = mMessagesAdapter.getItem(position);

                if (mMessagesAdapter.isSelectionActive()) {
                    selectMessage(view, messageId);
                } else {
                    Intent intent = new Intent(MessagesListActivity.this, SingleMessageActivity.class);
                    intent.putExtra(KulloConstants.MESSAGE_ID, messageId);
                    startActivity(intent);
                }
            }
            @Override
            public void onItemLongPress(View view, int position) {
                long messageId = mMessagesAdapter.getItem(position);
                selectMessage(view, messageId);
            }
        }));

        mConversationEmptyLabel = (TextView) findViewById(R.id.empty_list_label);
        updateEmptyLabel();
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

    // Called before onResume()
    // http://stackoverflow.com/questions/4253118/is-onresume-called-before-onactivityresult
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == KulloConstants.REQUEST_CODE_NEW_MESSAGE && resultCode == RESULT_OK) {
            // Avoid refreshing list here because that is done in onResume() anyways -simon
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        RuntimeAssertion.require(mMessagesAdapter != null);

        // Fill adapter with the most recent data
        mMessagesAdapter.updateDataSet();

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void updateSenderAvatarViewsInHeader() {
        mAvatarsRow = (LinearLayout) findViewById(R.id.avatars_row);
        if (SHOW_AVATAR_ROW) {
            mAvatarsRow.setVisibility(View.VISIBLE);

            if (mAvatarsRow.getChildCount() > 0) {
                mAvatarsRow.removeAllViews();
            }

            final int dp40 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
            final int dp8 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
            final LayoutParams params = new LayoutParams(dp40, dp40);
            params.setMargins(0, 0, dp8, 0);

            /*
            ArrayList<Bitmap> senderAvatars = KulloConnector.get().getConversationAvatars(this, mConversationId);
            for (Bitmap avatar : senderAvatars) {
                CircleImageView circleImageView = new CircleImageView(this);
                circleImageView.setImageBitmap(avatar);
                circleImageView.setLayoutParams(params);
                mAvatarsRow.addView(circleImageView);
            }
            */
        } else {
            mAvatarsRow.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMessageAttachmentsOpener.unregisterSaveFinishedListenerObserver();
        KulloConnector.get().removeListenerObserver(
                DownloadAttachmentsForMessageListenerObserver.class,
                mDownloadAttachmentsFinishedObserver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_refresh:
                mSwipeLayout.setEnabled(false);
                mSwipeLayout.setRefreshing(true);
                KulloConnector.get().syncKullo();
                return true;
            case R.id.action_toggle_message_size:
                toggleMessageSize();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.appbar_menu_messages_list, menu);

        if (mMessagesAdapter.isShowCardsExpanded()) {
            menu.findItem(R.id.action_toggle_message_size).setIcon(R.drawable.kullo_unfold_less);
        } else {
            menu.findItem(R.id.action_toggle_message_size).setIcon(R.drawable.kullo_unfold_more);
        }

        return true;
    }

    private void toggleMessageSize () {
        mMessagesAdapter.toggleMessageSize();
        invalidateOptionsMenu();
    }

    public void replyClicked(View v) {
        if (KulloConnector.get().userSettingsAreValidForSync()) {
            Intent intent = new Intent(this, ComposeActivity.class);
            intent.putExtra(KulloConstants.CONVERSATION_ID, mConversationId);
            startActivityForResult(intent, KulloConstants.REQUEST_CODE_NEW_MESSAGE);
        } else {
            showDialogToShowUserSettingsForCompletion();
        }
    }

    private void showDialogToShowUserSettingsForCompletion() {
        mShowSettingsDialog = new MaterialDialog.Builder(this)
                .title(R.string.new_message_settings_incomplete_dialog_title)
                .content(R.string.new_message_settings_incomplete_dialog_content)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        startActivity(new Intent(MessagesListActivity.this, SettingsActivity.class));
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        mShowSettingsDialog.dismiss();
                    }
                })
                .show();
    }

    private void registerSyncFinishedListenerObserver() {
        // avoid any unnecessary work when fragment's activity is paused (mIsPaused == true)
        mSyncerRunListenerObserver = new SyncerRunListenerObserver() {
            @Override
            public void draftAttachmentsTooBig(long convId) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mSwipeLayout != null) {
                            // mSwipeLayout.setEnabled(mAppbarOffset == 0);
                            mSwipeLayout.setEnabled(true);
                            mSwipeLayout.setRefreshing(false);
                        }
                    }
                });
            }

            @Override
            public void finished() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mSwipeLayout != null) {
                            // mSwipeLayout.setEnabled(mAppbarOffset == 0);
                            mSwipeLayout.setEnabled(true);
                            mSwipeLayout.setRefreshing(false);
                        }
                    }
                });
            }

            @Override
            public void error(String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mSwipeLayout != null) {
                            // mSwipeLayout.setEnabled(mAppbarOffset == 0);
                            mSwipeLayout.setEnabled(true);
                            mSwipeLayout.setRefreshing(false);
                        }
                    }
                });
            }
        };
        KulloConnector.get().addListenerObserver(
                SyncerRunListenerObserver.class,
                mSyncerRunListenerObserver);
    }

    private void unregisterSyncFinishedListenerObserver() {
        KulloConnector.get().removeListenerObserver(
                SyncerRunListenerObserver.class,
                mSyncerRunListenerObserver);
    }

    // CONTEXT MENU
    private void selectMessage(View view, final Long messageId) {
        mMessagesAdapter.toggleSelectedItem(messageId);
        if (!mMessagesAdapter.isSelectionActive()) {
            mActionMode.finish();
            return;
        }
        if (mActionMode == null) {
            mActionMode = startActionMode(new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    MenuInflater inflater = mode.getMenuInflater();
                    inflater.inflate(R.menu.appbar_menu_message_actions, menu);
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.action_delete:
                            List<Long> selectedMessageIdsCopy = new ArrayList<>(mMessagesAdapter.getSelectedItems());
                            for (long selectedMessageId : selectedMessageIdsCopy) {
                                KulloConnector.get().removeMessage(selectedMessageId);
                            }
                            KulloConnector.get().syncKullo();

                            mode.finish();
                            return true;
                        default:
                            return false;
                    }
                }
                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    mMessagesAdapter.clearSelectedItems();
                    mActionMode = null;
                }
            });
        }

        // update action bar title
        final String title = String.format(
                getResources().getString(R.string.title_n_selected),
                mMessagesAdapter.getSelectedItemsCount());
        mActionMode.setTitle(title);
    }

    public void clearSelection() {
        if (mActionMode != null) {
            mMessagesAdapter.clearSelectedItems();
            mActionMode.finish();
        }
    }
}
