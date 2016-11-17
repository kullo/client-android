/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration;

import net.kullo.android.R;
import net.kullo.android.kulloapi.CreateSessionResult;
import net.kullo.android.kulloapi.CreateSessionState;
import net.kullo.android.kulloapi.DialogMaker;
import net.kullo.android.kulloapi.KulloUtils;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.AvatarUtils;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.android.notifications.GcmConnector;
import net.kullo.android.observers.eventobservers.ConversationsEventObserver;
import net.kullo.android.observers.listenerobservers.SyncerListenerObserver;
import net.kullo.android.screens.conversationslist.ConversationsAdapter;
import net.kullo.android.ui.DividerDecoration;
import net.kullo.android.ui.RecyclerItemClickListener;
import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.NetworkError;
import net.kullo.libkullo.api.SyncProgress;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class ConversationsListActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "ConversationsListAct."; // max. 23 chars
    private Toolbar mToolbar;
    private NavigationView mNavigationView;
    private CircleImageView mNavigationHeaderAvatarView;
    private TextView mNavigationHeaderNameView;
    private TextView mNavigationHeaderAddressView;
    private DrawerLayout mDrawerLayout;
    private MenuItem mPreviousMenuItemNavigationView;
    protected RecyclerView mRecyclerView;
    private ConversationsAdapter mAdapter;
    private SwipeRefreshLayout mSwipeLayout;
    private MaterialProgressBar mProgressBarDeterminate;
    private MaterialProgressBar mProgressBarIndeterminate;
    private boolean mIsPaused;
    private ConversationsEventObserver mConversationsEventObserver;
    private SyncerListenerObserver mSyncerListenerObserver;
    private ActionMode mActionMode = null;
    private boolean mIsAtTopScrollPosition = true;


    //LIFECYCLE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final CreateSessionResult result = SessionConnector.get().createActivityWithSession(this);
        if (result.state == CreateSessionState.NO_CREDENTIALS) return;

        setContentView(R.layout.activity_conversations_list);

        Ui.prepareActivityForTaskManager(this);
        mToolbar = Ui.setupActionbar(this, false);
        setTitle(R.string.menu_conversations);
        setupLayout();
        setupSwipeRefreshLayout();

        if (result.state == CreateSessionState.CREATING) {
            RuntimeAssertion.require(result.task != null);
            result.task.waitUntilDone();
        }

        GcmConnector.get().fetchAndRegisterToken(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        RuntimeAssertion.require(SessionConnector.get().sessionAvailable());

        updateAccountInfoInNavigationHeader();
        registerConversationsEventObserver();
        registerSyncListenerObserver();

        SessionConnector.get().syncIfNecessary();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        GcmConnector.get().removeAllNotifications(this);

        handleSyncFromIntent();

        mIsPaused = false;

        if (SessionConnector.get().isSyncing()) {
            updateSwipeLayoutEnabled();
            mSwipeLayout.setRefreshing(false);

            mProgressBarIndeterminate.setVisibility(View.VISIBLE);
            mProgressBarDeterminate.setVisibility(View.GONE);
        } else {
            updateSwipeLayoutEnabled();
            mSwipeLayout.setRefreshing(false);

            mProgressBarIndeterminate.setVisibility(View.GONE);
            mProgressBarDeterminate.setVisibility(View.GONE);
        }

        reloadConversationsList();
    }

    @Override
    public void onPause() {
        super.onPause();
        // make sure that selection is clear if we leave this view
        clearSelection();
        mIsPaused = true;
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Throw away avatar bitmap in order to save memory. This is reloaded
        // in onStart anyway.
        mNavigationHeaderAvatarView.setImageBitmap(null);

        unregisterSyncFinishedListenerObserver();
        unregisterConversationsEventObserver();
    }

    private void updateSwipeLayoutEnabled() {
        mSwipeLayout.setEnabled(
                !SessionConnector.get().isSyncing()
                && (mIsAtTopScrollPosition || mAdapter.getItemCount() == 0)
        );
    }

    private void handleSyncFromIntent() {
        String action = getIntent().getAction();
        if (action != null && action.equals(KulloConstants.ACTION_SYNC)) {
            SessionConnector.get().sync();

            // reset action so that it is not executed multiple times
            getIntent().setAction(null);
        }
    }

    private void setupLayout() {
        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);
        RuntimeAssertion.require(mNavigationView != null);
        mNavigationView.setNavigationItemSelectedListener(this);

        View navigationHeaderView = mNavigationView.inflateHeaderView(R.layout.navigation_view_header);
        RuntimeAssertion.require(navigationHeaderView != null);

        mNavigationHeaderAvatarView = (CircleImageView) navigationHeaderView.findViewById(R.id.avatar);
        mNavigationHeaderNameView = (TextView) navigationHeaderView.findViewById(R.id.username);
        mNavigationHeaderAddressView = (TextView) navigationHeaderView.findViewById(R.id.address);
        RuntimeAssertion.require(mNavigationHeaderAvatarView != null);
        RuntimeAssertion.require(mNavigationHeaderNameView != null);
        RuntimeAssertion.require(mNavigationHeaderAddressView != null);

        // Initializing Drawer Layout and ActionBarToggle
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, mToolbar, R.string.open_drawer, R.string.close_drawer) {
            @Override
            public void onDrawerClosed(View drawerView) {
                // Code here will be triggered once the drawer closes
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Code here will be triggered once the drawer open
                updateAccountInfoInNavigationHeader();
                super.onDrawerOpened(drawerView);
            }
        };

        // Setting the actionbarToggle to drawer layout
        mDrawerLayout.setDrawerListener(actionBarDrawerToggle);

        // calling sync state to show menu icon
        actionBarDrawerToggle.syncState();

        // Select conversations menu item
        mNavigationView.getMenu().getItem(0).setCheckable(true);
        mNavigationView.getMenu().getItem(0).setChecked(true);
        mPreviousMenuItemNavigationView = mNavigationView.getMenu().getItem(0);

        //set up recycler view
        mRecyclerView = (RecyclerView) findViewById(R.id.conversations_recycler_view);
        final LinearLayoutManager llm = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(llm);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                mIsAtTopScrollPosition = llm.findFirstCompletelyVisibleItemPosition() == 0;
                updateSwipeLayoutEnabled();
            }
        });

        mAdapter = new ConversationsAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        // Add the sticky headers decoration
        final StickyRecyclerHeadersDecoration headersDecor = new StickyRecyclerHeadersDecoration(mAdapter);
        mRecyclerView.addItemDecoration(headersDecor);

        // Add decoration for dividers between list items
        int dividerLeftMargin = getResources().getDimensionPixelSize(R.dimen.md_additions_list_divider_margin_left);
        mRecyclerView.addItemDecoration(new DividerDecoration(this, dividerLeftMargin));

        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(mRecyclerView) {
            @Override
            public void onItemClick(View view, int position) {
                long conversationId = mAdapter.getItem(position);

                if (mAdapter.isSelectionActive()) {
                    selectConversation(conversationId);
                } else {
                    Intent intent = new Intent(ConversationsListActivity.this, MessagesListActivity.class);
                    intent.putExtra(MessagesListActivity.CONVERSATION_ID, conversationId);
                    startActivity(intent);
                }
            }

            @Override
            public void onItemLongPress(View view, int position) {
                long conversationId = mAdapter.getItem(position);
                selectConversation(conversationId);
            }
        });
    }

    private void registerConversationsEventObserver() {
        mConversationsEventObserver = new ConversationsEventObserver() {
            @Override
            public void conversationAdded(long conversationId) {
                Log.d(TAG, "Got a new conversation. Refresh view!");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        reloadConversationsList();
                    }
                });
            }

            @Override
            public void conversationChanged(long conversationId) {
                Log.d(TAG, "Conversation changed. Refresh View!");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        reloadConversationsList();
                    }
                });
            }

            @Override
            public void conversationRemoved(long conversationId) {
                Log.d(TAG, "Conversation deleted. Refresh view!");
                runOnUiThread(new Runnable() {
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
    }

    private void unregisterConversationsEventObserver() {
        SessionConnector.get().removeEventObserver(
                ConversationsEventObserver.class,
                mConversationsEventObserver);
    }

    private void registerSyncListenerObserver() {
        // avoid any unnecessary work when fragment's activity is paused (mIsPaused == true)
        mSyncerListenerObserver = new SyncerListenerObserver() {
            @Override
            public void started() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateSwipeLayoutEnabled();
                        mSwipeLayout.setRefreshing(false);

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
                        updateSwipeLayoutEnabled();
                        mSwipeLayout.setRefreshing(false);

                        if (KulloUtils.showSyncProgressAsBar(progress)) {
                            int percent = Math.round(100 * ((float) progress.getCountProcessed() / progress.getCountTotal()));

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
                Log.d(TAG, "Update conversations view after sync ...");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateSwipeLayoutEnabled();
                        mSwipeLayout.setRefreshing(false);

                        mProgressBarDeterminate.setVisibility(View.GONE);
                        mProgressBarIndeterminate.setVisibility(View.GONE);

                        if (!mIsPaused) reloadConversationsList();
                    }
                });
            }

            @Override
            public void error(final NetworkError error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateSwipeLayoutEnabled();
                        mSwipeLayout.setRefreshing(false);

                        mProgressBarDeterminate.setVisibility(View.GONE);
                        mProgressBarIndeterminate.setVisibility(View.GONE);

                        Toast.makeText(ConversationsListActivity.this,
                                DialogMaker.getTextForNetworkError(ConversationsListActivity.this, error),
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

    public void startConversationButtonClicked(View view) {
        startActivity(new Intent(this, StartConversationActivity.class));
    }

    private void updateAccountInfoInNavigationHeader() {
        mNavigationHeaderNameView.setText(SessionConnector.get().getClientName());
        mNavigationHeaderAddressView.setText(SessionConnector.get().getClientAddressAsString());

        byte[] avatar = SessionConnector.get().getClientAvatar();
        if (avatar != null && avatar.length > 0) {
            mNavigationHeaderAvatarView.setImageBitmap(AvatarUtils.avatarToBitmap(avatar));
            mNavigationHeaderAvatarView.setVisibility(View.VISIBLE);
        } else {
            mNavigationHeaderAvatarView.setVisibility(View.INVISIBLE);
        }
    }

    // This method will get triggered on item click in navigation menu
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        menuItem.setCheckable(true);
        menuItem.setChecked(true);

        // Check to see which item was being clicked and perform appropriate action
        switch (menuItem.getItemId()) {
            case R.id.menuitem_conversations:
                // nothing to do here: drawer will be closed on item selected, and we're already in the proper activity
                break;
            case R.id.menuitem_settings:
                Intent intentSettings = new Intent(this, SettingsActivity.class);
                startActivity(intentSettings);
                break;
            case R.id.menuitem_account:
                Intent intentAccount = new Intent(this, AccountActivity.class);
                startActivity(intentAccount);
                break;
            case R.id.menuitem_feedback:
                Intent intent = new Intent(this, ComposeActivity.class);
                intent.putExtra(KulloConstants.CONVERSATION_RECIPIENT, "hi#kullo.net");
                startActivity(intent);
                break;
            case R.id.menuitem_about:
                Intent intentAbout = new Intent(this, AboutActivity.class);
                startActivity(intentAbout);
                break;
            case R.id.menuitem_logout:
                closeInbox();
                return true;
            default:
                RuntimeAssertion.fail("Unhandled menu item");
        }

        // deselect previous menu item
        if (mPreviousMenuItemNavigationView != null) {
            mPreviousMenuItemNavigationView.setChecked(false);
            mNavigationView.invalidate();
        }

        // remember currently selected menu item to deselect it later - workaround design library issue
        mPreviousMenuItemNavigationView = menuItem;

        // Closing drawer on item click
        mDrawerLayout.closeDrawers();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.appbar_menu_conversations_list, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection, if it doesn't land here, the activity might have snatched it
        switch (item.getItemId()) {
            case R.id.action_refresh:
                SessionConnector.get().sync();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void closeInbox() {
        startActivity(new Intent(this, LeaveInboxActivity.class));
        finish();
    }

    private void setupSwipeRefreshLayout() {
        // Switching between indeterminate and determinate is entirely broken in
        // me.zhanghai.android.materialprogressbar.MaterialProgressBar. So use two views.
        mProgressBarDeterminate = (MaterialProgressBar) findViewById(R.id.progressbar_determinate);
        mProgressBarIndeterminate = (MaterialProgressBar) findViewById(R.id.progressbar_indeterminate);

        mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                SessionConnector.get().sync();
            }
        });
    }

    private void setVisibilityControlsIfListIsEmpty() {
        ImageView swipeToRefreshImage = (ImageView) findViewById(R.id.swipe_to_refresh_image);
        TextView swipeToRefreshText = (TextView) findViewById(R.id.swipe_to_refresh_text);

        if (mAdapter.getItemCount() == 0) {
            swipeToRefreshImage.setVisibility(View.VISIBLE);
            swipeToRefreshText.setVisibility(View.VISIBLE);
        } else {
            swipeToRefreshImage.setVisibility(View.GONE);
            swipeToRefreshText.setVisibility(View.GONE);
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

    private void selectConversation(final Long conversationId) {
        // (de)select conversation
        mAdapter.toggleSelectedItem(conversationId);
        if (!mAdapter.isSelectionActive()) {
            mActionMode.finish();
            return;
        }

        if (mActionMode == null) {
            // start new action mode
            mActionMode = startActionMode(new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    MenuInflater inflater = mode.getMenuInflater();
                    inflater.inflate(R.menu.appbar_menu_message_actions, menu);
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    Ui.setColorStatusBarArrangeHeader(ConversationsListActivity.this);
                    return true;
                }

                @Override
                public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.action_delete: {
                            int messagesToBeRemovedCount = 0;
                            final List<Long> conversationsToBeRemoved = new ArrayList<>();
                            for (long conversationId : mAdapter.getSelectedItems()) {
                                conversationsToBeRemoved.add(conversationId);
                                messagesToBeRemovedCount += SessionConnector.get().getMessageCount(conversationId);
                            }

                            if (messagesToBeRemovedCount > 0) {
                                new MaterialDialog.Builder(ConversationsListActivity.this)
                                        .title(R.string.conversations_confirm_delete_conversations_title)
                                        .content(String.format(
                                                getString(R.string.conversations_confirm_delete_conversations_body),
                                                conversationsToBeRemoved.size(),
                                                messagesToBeRemovedCount))
                                        .positiveText(R.string.action_delete)
                                        .negativeText(R.string.cancel)
                                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                SessionConnector.get().removeConversations(conversationsToBeRemoved);
                                                SessionConnector.get().sync();
                                                mode.finish();
                                            }
                                        })
                                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                                            @Override
                                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                dialog.dismiss();
                                                mode.finish();
                                            }
                                        })
                                        .show();
                            } else {
                                // All conversations are empty
                                // Don't sync here because conversations without messages cannot be synced
                                SessionConnector.get().removeConversations(conversationsToBeRemoved);
                                final String text = String.format(
                                        getResources().getString(R.string.conversations_toast_n_empty_deleted),
                                        conversationsToBeRemoved.size());
                                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
                                mode.finish();
                            }
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
                getResources().getString(R.string.title_n_selected),
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
