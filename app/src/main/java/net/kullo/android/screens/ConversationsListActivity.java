/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.kullo.android.R;
import net.kullo.android.application.KulloActivity;
import net.kullo.android.kulloapi.ConversationsComparatorDsc;
import net.kullo.android.kulloapi.CreateSessionResult;
import net.kullo.android.kulloapi.CreateSessionState;
import net.kullo.android.kulloapi.DialogMaker;
import net.kullo.android.kulloapi.KulloUtils;
import net.kullo.android.kulloapi.LockedSessionCallback;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.AvatarUtils;
import net.kullo.android.littlehelpers.Formatting;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.android.notifications.GcmConnector;
import net.kullo.android.observers.eventobservers.ConversationsEventObserver;
import net.kullo.android.observers.listenerobservers.SyncerListenerObserver;
import net.kullo.android.screens.conversationslist.ConversationViewHolder;
import net.kullo.android.screens.conversationslist.ConversationsAdapter;
import net.kullo.android.screens.conversationslist.ConversationsSectionHeaderViewHolder;
import net.kullo.android.thirdparty.DynamicSectionsAdapter;
import net.kullo.android.thirdparty.NullViewHolder;
import net.kullo.android.ui.RecyclerItemClickListener;
import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.AccountInfo;
import net.kullo.libkullo.api.NetworkError;
import net.kullo.libkullo.api.Session;
import net.kullo.libkullo.api.SyncProgress;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;
import io.github.dialogsforandroid.DialogAction;
import io.github.dialogsforandroid.MaterialDialog;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

@SuppressWarnings("RedundantIfStatement")
public class ConversationsListActivity extends KulloActivity implements
        NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "ConversationsListAct."; // max. 23 chars

    @BindView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    private Toolbar mToolbar;
    private ConversationsEventObserver mConversationsEventObserver;
    private SyncerListenerObserver mSyncerListenerObserver;

    // MESSAGES LIST

    @BindView(R.id.swipe_refresh_layout) SwipeRefreshLayout mSwipeLayout;
    @BindView(R.id.conversations_recycler_view) RecyclerView mRecyclerView;
    // MaterialProgressBar does not support switching between indeterminate and determinate, so use two views
    @BindView(R.id.progressbar_determinate) MaterialProgressBar mProgressBarDeterminate;
    @BindView(R.id.progressbar_indeterminate) MaterialProgressBar mProgressBarIndeterminate;
    private ConversationsAdapter mConversationsAdapter;
    private DynamicSectionsAdapter mAdapter;
    private ActionMode mActionMode = null;
    @BindView(R.id.empty_state_view) View emptyStateView;

    // DRAWER

    @BindView(R.id.navigation_view) NavigationView mNavigationView;
    private MenuItem mPreviousMenuItemNavigationView;

    private View mNavigationHeaderView;
    private CircleImageView mNavigationHeaderAvatarView;
    private TextView mNavigationHeaderNameView;
    private TextView mNavigationHeaderAddressView;
    private ImageView mNavigationHeaderArrowIcon;

    private boolean mNavigationOverlayActive = false;
    @Nullable private AccountInfo mAccountInfo = null;
    @BindView(R.id.nav_overlay) LinearLayout mOverlay;
    @BindView(R.id.nav_overlay_network_error) View mOverlayNetworkError;
    @BindView(R.id.nav_overlay_content) View mOverlayContent;
    @BindView(R.id.nav_overlay_account_info_storage) TextView mOverlayTextStorage;
    @BindView(R.id.nav_overlay_account_info_plan) TextView mOverlayTextPlan;


    //LIFECYCLE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final CreateSessionResult result = SessionConnector.get().createActivityWithSession(this);
        if (result.state == CreateSessionState.NO_CREDENTIALS) return;

        setContentView(R.layout.activity_conversations_list);

        Ui.prepareActivityForTaskManager(this);
        Ui.setStatusBarColor(this, false, Ui.LayoutType.DrawerLayout);
        mToolbar = Ui.setupActionbar(this, false);
        setTitle(R.string.menu_conversations);
        ButterKnife.bind(this);
        setupNavigationDrawer();
        setupMainLayout();
        setupSwipeRefreshLayout();

        if (result.state == CreateSessionState.CREATING) {
            RuntimeAssertion.require(result.task != null);
            result.task.waitUntilDone();
        }

        GcmConnector.get().ensureSessionHasTokenRegisteredAsync();

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                // isDrawerOpen() does not return correct value in onCreate
                if (mDrawerLayout.isDrawerOpen(mNavigationView)) {
                    // when drawer is open at activity start,
                    // onDrawerOpened is not called
                    loadAccountInfoFromServer();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        RuntimeAssertion.require(SessionConnector.get().sessionAvailable());

        updateProfileInNavigationHeader();
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

        if (SessionConnector.get().isSyncing()) {
            updateSwipeLayout(true);

            mProgressBarIndeterminate.setVisibility(View.VISIBLE);
            mProgressBarDeterminate.setVisibility(View.GONE);
        } else {
            updateSwipeLayout(false);

            mProgressBarIndeterminate.setVisibility(View.GONE);
            mProgressBarDeterminate.setVisibility(View.GONE);
        }

        updateArrowIcon();
        reloadConversationsList();
    }

    @Override
    public void onPause() {
        super.onPause();
        // make sure that selection is clear if we leave this view
        stopActionMode();
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

    private void updateSwipeLayout(boolean isSyncing) {
        mSwipeLayout.setEnabled(!isSyncing);
    }

    private void handleSyncFromIntent() {
        String action = getIntent().getAction();
        if (action != null && action.equals(KulloConstants.ACTION_SYNC)) {
            SessionConnector.get().sync();

            // reset action so that it is not executed multiple times
            getIntent().setAction(null);
        }
    }

    private void setupNavigationDrawer() {
        mNavigationView.setNavigationItemSelectedListener(this);

        mNavigationHeaderView = mNavigationView.inflateHeaderView(R.layout.navigation_view_header);
        RuntimeAssertion.require(mNavigationHeaderView != null);

        mNavigationHeaderAvatarView = (CircleImageView) mNavigationHeaderView.findViewById(R.id.avatar);
        mNavigationHeaderNameView = (TextView) mNavigationHeaderView.findViewById(R.id.username);
        mNavigationHeaderAddressView = (TextView) mNavigationHeaderView.findViewById(R.id.address);
        mNavigationHeaderArrowIcon = (ImageView) mNavigationHeaderView.findViewById(R.id.account_menu_toggle_icon);
        RuntimeAssertion.require(mNavigationHeaderAvatarView != null);
        RuntimeAssertion.require(mNavigationHeaderNameView != null);
        RuntimeAssertion.require(mNavigationHeaderAddressView != null);
        RuntimeAssertion.require(mNavigationHeaderArrowIcon != null);

        // Initializing Drawer Layout and ActionBarToggle
        Ui.setStatusBarColor(mDrawerLayout);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
            this, mDrawerLayout, mToolbar, R.string.open_drawer, R.string.close_drawer) {
            @Override
            public void onDrawerOpened(View drawerView) {
                updateProfileInNavigationHeader();
                loadAccountInfoFromServer();
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                hideNavigationOverlay(false);
            }
        };
        mDrawerLayout.addDrawerListener(actionBarDrawerToggle);

        // When called in onPostCreate, hamburger menu is missing
        actionBarDrawerToggle.syncState();

        // Select conversations menu item
        mNavigationView.getMenu().getItem(0).setCheckable(true);
        mNavigationView.getMenu().getItem(0).setChecked(true);
        mPreviousMenuItemNavigationView = mNavigationView.getMenu().getItem(0);

        // Overlay: set top position
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                int headerHeight = mNavigationHeaderView.getHeight();

                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, headerHeight, 0, 0);
                params.height = 0;
                mOverlay.setLayoutParams(params);
            }
        });
    }

    private void showNavigationOverlay() {
        mNavigationOverlayActive = true;
        updateArrowIcon();
        updateAccountInfoViews();

        int headerHeight = mNavigationHeaderView.getHeight();
        int overlayHeight = mNavigationView.getHeight() - headerHeight;
        navigationOverlayAnimateTo(overlayHeight);
    }

    private void hideNavigationOverlay(boolean animated) {
        mNavigationOverlayActive = false;
        updateArrowIcon();

        int targetHeight = 0;
        if (animated) {
            navigationOverlayAnimateTo(targetHeight);
        } else {
            ViewGroup.LayoutParams params = mOverlay.getLayoutParams();
            params.height = targetHeight;
            mOverlay.setLayoutParams(params);
        }
    }

    private void navigationOverlayAnimateTo(int targetHeight) {
        ValueAnimator anim = ValueAnimator.ofInt(mOverlay.getHeight(), targetHeight);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                Integer value = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams params = mOverlay.getLayoutParams();
                params.height = value;
                mOverlay.setLayoutParams(params);
            }
        });
        anim.setDuration(250);
        anim.start();
    }

    private void setupMainLayout() {
        //set up recycler view
        final LinearLayoutManager llm = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(llm);

        mConversationsAdapter = new ConversationsAdapter(this);
        mConversationsAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                updateEmptyStateView();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                updateEmptyStateView();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                updateEmptyStateView();
            }
        });

        mAdapter = new DynamicSectionsAdapter<ConversationViewHolder, ConversationsSectionHeaderViewHolder, NullViewHolder>(mConversationsAdapter) {

            @Override
            protected long getSectionId(int originalAdapterPosition) {
                long conversationId = mConversationsAdapter.getItem(originalAdapterPosition);
                DateTime latestMessageTimestamp = SessionConnector.get().getLatestMessageTimestamp(conversationId);

                if (latestMessageTimestamp.equals(SessionConnector.get().emptyConversationTimestamp())) {
                    return Long.MAX_VALUE;
                } else {
                    LocalDateTime localLatestMessageTimestamp = new LocalDateTime(latestMessageTimestamp, DateTimeZone.getDefault());
                    LocalDate today = new LocalDate(DateTimeZone.getDefault());

                    // The header id must change whenever the date of the latest message changed (t1)
                    // as well as when the current date changes (t2) to invalidate "today" or "yesterday" labels.
                    // This is going to work until year 2038, then we need to cut non-significant bits.
                    long t1 = localLatestMessageTimestamp.toLocalDate().toDateTimeAtStartOfDay().getMillis() / 1000; // 31 bit
                    long t2 = today.toDateTimeAtStartOfDay().getMillis() / 1000; // 31 bit
                    return (t1 << 31) | t2;
                }
            }

            @Override
            protected boolean sectionHasHeader(long sectionId) {
                return true;
            }

            @Override
            protected ConversationsSectionHeaderViewHolder onCreateSectionHeaderViewHolder(ViewGroup parent) {
                View itemView = LayoutInflater.
                    from(parent.getContext()).
                    inflate(R.layout.row_conversation_header, parent, false);
                return new ConversationsSectionHeaderViewHolder(itemView);
            }

            @Override
            protected void onBindHeaderViewHolder(ConversationsSectionHeaderViewHolder vh, long sectionId) {
                TextView textView = (TextView) vh.itemView;
                Long conversationId = mConversationsAdapter.getItem(getOriginalPositionForFirstItemInSection(sectionId));

                DateTime latestMessageTimestamp = SessionConnector.get().getLatestMessageTimestamp(conversationId);

                if (latestMessageTimestamp.getMillis() == SessionConnector.get().emptyConversationTimestamp().getMillis()) {
                    textView.setText(R.string.empty_conversation_title);
                }
                else {
                    textView.setText(Formatting.getLocalDateText(latestMessageTimestamp));
                }
            }
        };

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(mRecyclerView) {
            @Override
            public void onItemClick(View view, int position) {
                int originalAdapterPosition = mAdapter.getOriginalPositionForPosition(position);
                if (originalAdapterPosition < 0) return;
                long conversationId = mConversationsAdapter.getItem(originalAdapterPosition);

                if (mConversationsAdapter.isSelectionActive()) {
                    selectConversation(conversationId);
                } else {
                    Intent intent = new Intent(ConversationsListActivity.this, MessagesListActivity.class);
                    intent.putExtra(KulloConstants.CONVERSATION_ID, conversationId);
                    startActivity(intent);
                }
            }

            @Override
            public void onItemLongPress(View view, int position) {
                int originalAdapterPosition = mAdapter.getOriginalPositionForPosition(position);
                if (originalAdapterPosition < 0) return;
                long conversationId = mConversationsAdapter.getItem(originalAdapterPosition);
                selectConversation(conversationId);
            }
        });
    }

    private void registerConversationsEventObserver() {
        mConversationsEventObserver = new ConversationsEventObserver() {
            @Override
            public void conversationAdded(final long conversationId) {
                SessionConnector.get().runWithLockedSession(new LockedSessionCallback() {
                    @Override
                    public void run(Session lockedSession) {
                        mConversationsAdapter.add(conversationId,
                            new ConversationsComparatorDsc(lockedSession));
                    }
                });
            }

            @Override
            public void conversationChanged(final long conversationId) {
                Log.d(TAG, "Conversation changed. Refresh View!");

                final boolean[] changeCouldBeHandled = {false};

                SessionConnector.get().runWithLockedSession(new LockedSessionCallback() {
                    @Override
                    public void run(Session lockedSession) {
                        changeCouldBeHandled[0] = mConversationsAdapter.tryHandleElementChanged(
                            conversationId, new ConversationsComparatorDsc(lockedSession));
                    }
                });

                if (!changeCouldBeHandled[0]) {
                    reloadConversationsList();
                }
            }

            @Override
            public void conversationRemoved(long conversationId) {
                Log.d(TAG, "Conversation deleted. Refresh view!");
                reloadConversationsList();
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
                Log.d(TAG, "Update conversations view after sync ...");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateSwipeLayout(false);

                        mProgressBarDeterminate.setVisibility(View.GONE);
                        mProgressBarIndeterminate.setVisibility(View.GONE);

                        //if (!mIsPaused) reloadConversationsList();
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

    private void updateProfileInNavigationHeader() {
        mNavigationHeaderNameView.setText(SessionConnector.get().getCurrentUserName());
        mNavigationHeaderAddressView.setText(SessionConnector.get().getCurrentUserAddress().toString());

        byte[] avatar = SessionConnector.get().getCurrentUserAvatar();
        if (avatar.length > 0) {
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
            case R.id.menuitem_profile_settings:
                startActivity(new Intent(this, ProfileSettingsActivity.class));
                break;
            case R.id.menuitem_masterkey:
                startActivity(new Intent(this, MasterKeyActivity.class));
                break;
            case R.id.menuitem_feedback:
                Intent intent = new Intent(this, ComposeActivity.class);
                intent.putExtra(KulloConstants.CONVERSATION_RECIPIENT, "hi#kullo.net");
                startActivity(intent);
                break;
            case R.id.menuitem_about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
            case R.id.menuitem_logout:
                leaveInboxClicked();
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
        setupSearchAction(menu.findItem(R.id.action_search), -1);
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

    public void leaveInboxClicked() {
        new MaterialDialog.Builder(this)
            .title(R.string.leave_inbox_confirmation_title)
            .content(String.format(
                getString(R.string.leave_inbox_confirmation_description),
                SessionConnector.get().getCurrentUserAddress()))
            .positiveText(R.string.leave_inbox_confirmation_positive)
            .negativeText(R.string.leave_inbox_confirmation_negative)
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                    ConversationsListActivity activity = ConversationsListActivity.this;
                    activity.startActivity(new Intent(activity, LeaveInboxActivity.class));
                    activity.finish();
                }
            })
            .show();
    }

    private void setupSwipeRefreshLayout() {
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
    }

    private void updateEmptyStateView() {
        if (mConversationsAdapter.getItemCount() == 0) {
            emptyStateView.setVisibility(View.VISIBLE);
        } else {
            emptyStateView.setVisibility(View.GONE);
        }
    }

    private void reloadConversationsList() {
        RuntimeAssertion.require(mConversationsAdapter != null);

        // When list is reloaded, selection is not preserved.
        // So stop action mode as well
        stopActionMode();

        List<Long> conversationIds = SessionConnector.get().getAllConversationIds(true);
        mConversationsAdapter.replaceAll(conversationIds);
        Log.d(TAG, "Replaced all conversations");
    }

    private void selectConversation(final Long conversationId) {
        // (de)select conversation
        mConversationsAdapter.toggleSelectedItem(conversationId);
        if (!mConversationsAdapter.isSelectionActive()) {
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
                    Ui.setStatusBarColor(ConversationsListActivity.this, true, Ui.LayoutType.DrawerLayout);
                    return true;
                }

                @Override
                public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.action_delete: {
                            int messagesToBeRemovedCount = 0;
                            final List<Long> conversationsToBeRemoved = new ArrayList<>();
                            for (long conversationId : mConversationsAdapter.getSelectedItems()) {
                                conversationsToBeRemoved.add(conversationId);
                                messagesToBeRemovedCount += SessionConnector.get().getMessageCount(conversationId);
                            }

                            if (messagesToBeRemovedCount > 0) {
                                String title = conversationsToBeRemoved.size() == 1
                                    ? getString(R.string.conversations_confirm_delete_title_1)
                                    : getString(R.string.conversations_confirm_delete_title_n);
                                String questionPart1 = conversationsToBeRemoved.size() == 1
                                    ? getString(R.string.conversations_confirm_delete_body_part1_1)
                                    : String.format(getString(R.string.conversations_confirm_delete_body_part1_n), conversationsToBeRemoved.size());
                                String questionPart2 = messagesToBeRemovedCount == 1
                                    ? getString(R.string.conversations_confirm_delete_body_part2_1)
                                    : String.format(getString(R.string.conversations_confirm_delete_body_part2_n), messagesToBeRemovedCount);
                                new MaterialDialog.Builder(ConversationsListActivity.this)
                                        .title(title)
                                        .content(questionPart1 + " " + questionPart2)
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
                                final String text = conversationsToBeRemoved.size() == 1
                                    ? getString(R.string.conversations_toast_deleting_empty_1)
                                    : String.format(getString(R.string.conversations_toast_deleting_empty_n), conversationsToBeRemoved.size());
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
                    mConversationsAdapter.clearSelectedItems();
                    Ui.setStatusBarColor(ConversationsListActivity.this, false, Ui.LayoutType.DrawerLayout);
                    mActionMode = null;
                }
            });
        }

        // update action bar title
        final String title = String.format(
            getResources().getString(R.string.actionmode_title_n_selected),
            mConversationsAdapter.getSelectedItemsCount());
        mActionMode.setTitle(title);
    }

    public void stopActionMode() {
        if (mActionMode != null) {
            mConversationsAdapter.clearSelectedItems();
            mActionMode.finish();
        }
    }

    public void toggleAccountSettingsClicked(View view) {
        if (!mNavigationOverlayActive) {
            showNavigationOverlay();
        } else {
            hideNavigationOverlay(true);
        }
    }

    private void loadAccountInfoFromServer() {
        SessionConnector.get().getAccountInfo(new SessionConnector.GetAccountInfoCallback() {
            @Override
            public void onDone(@Nullable final AccountInfo accountInfo) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // do not override old information in case of network error
                        if (accountInfo != null) {
                            mAccountInfo = accountInfo;
                        }

                        updateAccountInfoViews();
                    }
                });
            }
        });
    }

    @MainThread
    private void updateAccountInfoViews() {
        if (mAccountInfo != null) {
            mOverlayNetworkError.setVisibility(View.GONE);
            mOverlayContent.setVisibility(View.VISIBLE);

            final String storageText = Formatting.quotaInGib(
                mAccountInfo.getStorageUsed(), mAccountInfo.getStorageQuota());
            final String planText = mAccountInfo.getPlanName();
            mOverlayTextStorage.setText(storageText);
            mOverlayTextPlan.setText(planText);
        } else {
            mOverlayNetworkError.setVisibility(View.VISIBLE);
            mOverlayContent.setVisibility(View.GONE);
        }
    }

    private void updateArrowIcon() {
        mNavigationHeaderArrowIcon.setImageResource(mNavigationOverlayActive
            ? R.drawable.ic_arrow_drop_up_white_24dp
            : R.drawable.ic_arrow_drop_down_white_24dp);
    }

    public void retryGetAccountInfoClicked(View view) {
        loadAccountInfoFromServer();
    }

    public void openAccountSettingsClicked(View view) {
        RuntimeAssertion.require(mAccountInfo != null, "Button must not be visible when target url is not available");
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mAccountInfo.getSettingsUrl())));
    }
}
