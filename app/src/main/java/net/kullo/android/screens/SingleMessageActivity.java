/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import net.kullo.android.R;
import net.kullo.android.application.CommonDialogs;
import net.kullo.android.application.KulloApplication;
import net.kullo.android.kulloapi.CreateSessionResult;
import net.kullo.android.kulloapi.CreateSessionState;
import net.kullo.android.kulloapi.DialogMaker;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.Formatting;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.littlehelpers.TextViewContent;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.android.notifications.GcmConnector;
import net.kullo.android.observers.eventobservers.MessageAttachmentsDownloadedChangedEventObserver;
import net.kullo.android.observers.listenerobservers.SyncerListenerObserver;
import net.kullo.android.screens.singlemessage.AttachmentsAdapter;
import net.kullo.android.screens.singlemessage.MessageAttachmentsOpener;
import net.kullo.android.ui.NonScrollingGridLayoutManager;
import net.kullo.android.ui.RecyclerItemClickListener;
import net.kullo.android.ui.ScreenMetrics;
import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.Address;
import net.kullo.libkullo.api.AttachmentsBlockDownloadProgress;
import net.kullo.libkullo.api.NetworkError;
import net.kullo.libkullo.api.SyncProgress;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;
import io.github.dialogsforandroid.MaterialDialog;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class SingleMessageActivity extends AppCompatActivity {
    @SuppressWarnings("unused") private static final String TAG = "SingleMessageActivity";
    private long mMessageId;

    private MessageAttachmentsOpener mMessageAttachmentsOpener;
    private SyncerListenerObserver mDownloadAttachmentsFinishedObserver;
    private MessageAttachmentsDownloadedChangedEventObserver mMessageAttachmentsDownloadedChangedEventObserver;

    private static final DateTimeZone LOCAL_TIME_ZONE = DateTimeZone.getDefault();

    private AttachmentsAdapter mAttachmentsAdapter = null;
    @Nullable private ActionMode mActionMode = null;

    // Views
    @BindView(R.id.sender_avatar) CircleImageView mSenderAvatarImageView;
    @BindView(R.id.sender_name_organization) TextView mSenderNameOrganizationTextView;
    @BindView(R.id.sender_address) TextView mSenderAddressTextView;
    @BindView(R.id.message_date_row1) TextView mMessageDateRow1;
    @BindView(R.id.message_date_row2) TextView mMessageDateRow2;
    @BindView(R.id.message_content) TextView mMessageContentTextView;
    @BindView(R.id.attachments_list) RecyclerView mAttachmentsList;
    @BindView(R.id.content_bottom_padding_element) View mContentBottomPaddingElement;
    @BindView(R.id.footer_container) View mFooterContainer;
    @BindView(R.id.footer_button) ImageButton mToggleFooterButton;
    @BindView(R.id.footer_text) TextView mFooterTextView;

    private RecyclerView.LayoutManager mAttachmentsLayoutManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final CreateSessionResult result = SessionConnector.get().createActivityWithSession(this);
        if (result.state == CreateSessionState.NO_CREDENTIALS) return;

        setContentView(R.layout.activity_single_message);

        Intent intent = getIntent();
        mMessageId = intent.getLongExtra(KulloConstants.MESSAGE_ID, -1);
        RuntimeAssertion.require(mMessageId != -1);

        Ui.prepareActivityForTaskManager(this);
        Ui.setupActionbar(this);
        Ui.setStatusBarColor(this);
        ButterKnife.bind(this);

        mMessageAttachmentsOpener = new MessageAttachmentsOpener(this);

        registerObservers();
        setupAttachmentsList();

        if (result.state == CreateSessionState.CREATING) {
            RuntimeAssertion.require(result.task != null);
            result.task.waitUntilDone();
        }

        GcmConnector.get().ensureSessionHasTokenRegisteredAsync();

        populateMessageFields();
    }

    private void setupAttachmentsList() {
        mAttachmentsList.setNestedScrollingEnabled(false);
        mAttachmentsList.addOnItemTouchListener(new RecyclerItemClickListener(mAttachmentsList) {
            @Override
            public void onItemClick(View view, int position) {
                if (!SessionConnector.get().getMessageAttachmentsDownloaded(mMessageId)) {
                    SessionConnector.get().downloadAttachments(mMessageId);
                } else {
                    long attachmentId = mAttachmentsAdapter.getItem(position);
                    if (mAttachmentsAdapter.isSelectionActive()) {
                        toggleAttachmentSelection(attachmentId);
                    } else {
                        mMessageAttachmentsOpener.saveAndOpenAttachment(mMessageId, attachmentId);
                    }
                }
            }

            @Override
            public void onItemLongPress(View view, int position) {
                if (!SessionConnector.get().getMessageAttachmentsDownloaded(mMessageId)) return;

                long attachmentId = mAttachmentsAdapter.getItem(position);
                toggleAttachmentSelection(attachmentId);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        SessionConnector.get().setMessageRead(mMessageId);
    }

    @Override
    public void onResume() {
        super.onResume();
        GcmConnector.get().removeAllNotifications(this);
    }

    @Override
    protected void onStop() {
        stopActionMode();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterObservers();
    }

    private void registerObservers() {
        mDownloadAttachmentsFinishedObserver = new SyncerListenerObserver() {
            @Override
            public void started() {
            }

            @Override
            public void draftAttachmentsTooBig(long convId) {
            }

            @Override
            public void progressed(final SyncProgress progress) {
                if (progress.getIncomingAttachments().containsKey(mMessageId)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!SessionConnector.get().getMessageAttachmentsDownloaded(mMessageId)) {
                                AttachmentsBlockDownloadProgress attachmentProgress = progress.getIncomingAttachments().get(mMessageId);
                                int perMille = Formatting.perMilleRounded(
                                    attachmentProgress.getDownloadedBytes(),
                                    attachmentProgress.getTotalBytes());
                                setAttachmentsDownloadProgress(perMille);
                            }
                        }
                    });
                }
            }

            @Override
            public void finished() {
            }

            @Override
            public void error(final NetworkError error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(SingleMessageActivity.this,
                                DialogMaker.getTextForNetworkError(SingleMessageActivity.this, error),
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        };
        SessionConnector.get().addListenerObserver(
                SyncerListenerObserver.class,
                mDownloadAttachmentsFinishedObserver);

        mMessageAttachmentsDownloadedChangedEventObserver = new MessageAttachmentsDownloadedChangedEventObserver() {
            @MainThread
            @Override
            public void messageAttachmentsDownloadedChanged(final long messageId) {
                if (messageId == mMessageId) {
                    // Only not-downloaded -> downloaded is implemented at the moment
                    // so we assume the attachments are downloaded here
                    reloadAttachmentsList();
                }
            }
        };
        SessionConnector.get().addEventObserver(
                MessageAttachmentsDownloadedChangedEventObserver.class,
                mMessageAttachmentsDownloadedChangedEventObserver);

        mMessageAttachmentsOpener.registerSaveFinishedListenerObserver();
    }

    private void unregisterObservers() {
        mMessageAttachmentsOpener.unregisterSaveFinishedListenerObserver();
        SessionConnector.get().removeEventObserver(
                MessageAttachmentsDownloadedChangedEventObserver.class,
                mMessageAttachmentsDownloadedChangedEventObserver);
        SessionConnector.get().removeListenerObserver(
                SyncerListenerObserver.class,
                mDownloadAttachmentsFinishedObserver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                long conversationId = SessionConnector.get().getMessageConversation(mMessageId);
                final Intent upIntent = new Intent(this, MessagesListActivity.class);
                upIntent.putExtra(KulloConstants.CONVERSATION_ID, conversationId);
                NavUtils.navigateUpTo(this, upIntent);
                return true;
            }
            case R.id.action_write: {
                if (SessionConnector.get().userSettingsAreValidForSync()) {
                    long conversationId = SessionConnector.get().getMessageConversation(mMessageId);
                    Intent intent = new Intent(this, ComposeActivity.class);
                    intent.putExtra(KulloConstants.CONVERSATION_ID, conversationId);
                    startActivityForResult(intent, KulloConstants.REQUEST_CODE_NEW_MESSAGE);
                } else {
                    showDialogToShowUserSettingsForCompletion();
                }
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.appbar_menu_single_message, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode >= 1000) {
            mMessageAttachmentsOpener.handleActivityResult(requestCode, resultCode, data);
        }
    }

    public void populateMessageFields() {
        final DateTime dateReceived = SessionConnector.get().getMessageDateReceived(mMessageId);
        final String messageTextAsHtml = SessionConnector.get().getMessageTextAsHtml(mMessageId);
        final String messageFooter = SessionConnector.get().getMessageFooter(mMessageId);
        final Bitmap senderAvatar = SessionConnector.get().getSenderAvatar(this, mMessageId);
        final Address senderAddress = SessionConnector.get().getSenderAddress(mMessageId);
        final String senderName = SessionConnector.get().getSenderName(mMessageId);
        final String senderOrganization = SessionConnector.get().getSenderOrganization(mMessageId);
        final boolean isMe = (senderAddress.isEqualTo(SessionConnector.get().getCurrentUserAddress()));

        StringBuilder senderNameOrganization = new StringBuilder();
        senderNameOrganization.append(senderName);
        if (!senderOrganization.isEmpty()) {
            senderNameOrganization.append(" (");
            senderNameOrganization.append(senderOrganization);
            senderNameOrganization.append(")");
        }

        mMessageDateRow1.setText(getDateAsText(dateReceived));
        mMessageDateRow2.setText(getTimeAsText(dateReceived));
        mSenderAvatarImageView.setImageBitmap(senderAvatar);
        mSenderNameOrganizationTextView.setText(senderNameOrganization.toString());
        mSenderAddressTextView.setText(senderAddress.toString());
        mSenderAddressTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogMaker.makeForKulloAddress(SingleMessageActivity.this, senderAddress, isMe).show();
            }
        });

        setTitle(senderName);

        mMessageContentTextView.setMaxLines(Integer.MAX_VALUE);
        TextViewContent.injectHtmlIntoTextView(mMessageContentTextView, messageTextAsHtml, new TextViewContent.LinkClickedListener() {
                @Override
                protected void onClicked(Uri linkTarget) {
                    switch (linkTarget.getScheme()) {
                        case "http": case "https":
                            startActivity(new Intent(Intent.ACTION_VIEW, linkTarget));
                            break;
                        case "kulloInternal": {
                            String cutPart = "kulloInternal:";
                            String addressString = linkTarget.toString().replace(cutPart, "");
                            Address address = Address.create(addressString);
                            boolean isMe = (SessionConnector.get().getCurrentUserAddress().isEqualTo(address));
                            DialogMaker.makeForKulloAddress(SingleMessageActivity.this, address, isMe).show();
                            break;
                        }
                        default:
                            RuntimeAssertion.fail("Unknown scheme");
                    }
                }
            });

        if (messageFooter.isEmpty()) {
            hideFooterContainer();
        } else {
            mFooterTextView.setText(messageFooter);
            showFooterContainer(false);
        }

        // Set dummy layout manager to avoid error log "E/RecyclerView: No adapter attached; skipping layout"
        LinearLayoutManager dummyLayoutManager = new LinearLayoutManager(this);
        mAttachmentsList.setLayoutManager(dummyLayoutManager);

        // We need to wait for the RecyclerView layouting in order to have width available
        mAttachmentsList.post(new Runnable() {
            @Override
            public void run() {
                // Set layout manager before loading first data
                int columns = ScreenMetrics.getColumnsForComponent(mAttachmentsList, 100.0f);
                mAttachmentsLayoutManager = new NonScrollingGridLayoutManager(SingleMessageActivity.this, columns);
                mAttachmentsList.setLayoutManager(mAttachmentsLayoutManager);

                reloadAttachmentsList();
            }
        });
    }

    private void showDialogToShowUserSettingsForCompletion() {
        final MaterialDialog showSettingsDialog = CommonDialogs.buildShowSettingsDialog(this);
        showSettingsDialog.show();
    }


    // Set -1 to disable download progress
    @MainThread
    private void setAttachmentsDownloadProgress(int perMille) {
        List<View> views = new LinkedList<>();
        for (int i = 0; i < mAttachmentsLayoutManager.getChildCount(); ++i) {
            views.add(mAttachmentsLayoutManager.findViewByPosition(i));
        }

        if (perMille < 0) {
            for (View view : views) {
                view.findViewById(R.id.icon_default).setVisibility(View.VISIBLE);
                view.findViewById(R.id.icon_progress).setVisibility(View.GONE);
            }
        } else {
            for (View view : views) {
                view.findViewById(R.id.icon_default).setVisibility(View.GONE);

                MaterialProgressBar progressView = (MaterialProgressBar) view.findViewById(R.id.icon_progress);
                progressView.setVisibility(View.VISIBLE);
                progressView.setMax(1000);
                progressView.setProgress(perMille);
            }
        }
    }

    private void reloadAttachmentsList() {
        final ArrayList<Long> attachmentIds = SessionConnector.get().getMessageAttachmentsIds(mMessageId);
        final boolean attachmentsDownloaded = SessionConnector.get().getMessageAttachmentsDownloaded(mMessageId);

        if (attachmentIds.size() == 0) {
            mAttachmentsList.setVisibility(View.GONE);
            return;
        }

        mAttachmentsList.setVisibility(View.VISIBLE);

        mAttachmentsAdapter = new AttachmentsAdapter(SingleMessageActivity.this, mMessageId, attachmentsDownloaded);
        mAttachmentsList.setAdapter(mAttachmentsAdapter);

        if (attachmentsDownloaded) {
            mAttachmentsList.setEnabled(true);
        } else {
            mAttachmentsList.setEnabled(false);
        }
    }

    @NonNull
    private String getDateAsText(DateTime dateReceived) {
        LocalDateTime localDateReceived = new LocalDateTime(dateReceived, LOCAL_TIME_ZONE);
        return localDateReceived.toString(KulloApplication.sharedInstance.getShortDateFormatter());
    }

    @NonNull
    private String getTimeAsText(DateTime dateReceived) {
        LocalDateTime localDateReceived = new LocalDateTime(dateReceived, LOCAL_TIME_ZONE);
        return localDateReceived.toString(KulloApplication.sharedInstance.getShortTimeFormatter());
    }

    private void showFooterContainer(boolean footerExpanded) {
        mContentBottomPaddingElement.setVisibility(View.GONE);
        mFooterContainer.setVisibility(View.VISIBLE);

        if (footerExpanded) {
            mFooterTextView.setVisibility(View.VISIBLE);
            mToggleFooterButton.setImageResource(R.drawable.ic_expand_less_active_button_color_36dp);
            mToggleFooterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showFooterContainer(false);
                }
            });
        } else {
            mFooterTextView.setVisibility(View.GONE);
            mToggleFooterButton.setImageResource(R.drawable.ic_expand_more_active_button_color_36dp);
            mToggleFooterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showFooterContainer(true);
                }
            });
        }
    }

    private void hideFooterContainer() {
        mContentBottomPaddingElement.setVisibility(View.VISIBLE);
        mFooterContainer.setVisibility(View.GONE);
    }

    private void toggleAttachmentSelection(final long attachmentId) {
        mAttachmentsAdapter.toggleSelectedItem(attachmentId);

        if (!mAttachmentsAdapter.isSelectionActive()) {
            if (mActionMode != null) mActionMode.finish();
        } else {
            if (mActionMode == null) setupAttachmentsActionMode();

            // update action bar title
            final String title = String.format(
                getResources().getString(R.string.actionmode_title_n_selected),
                mAttachmentsAdapter.getSelectedItemsCount());
            mActionMode.setTitle(title);
            boolean singleItemActions = mAttachmentsAdapter.getSelectedItemsCount() == 1;
            mActionMode.getMenu().findItem(R.id.action_open_with)
                .setVisible(singleItemActions);
            mActionMode.getMenu().findItem(R.id.action_save_as)
                .setVisible(singleItemActions);
        }
    }

    private void setupAttachmentsActionMode() {
        mActionMode = startActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.appbar_menu_attachment_actions, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_save_as: {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            // Set contains only one element in this branch
                            long attachmentId = mAttachmentsAdapter.getSelectedItems().iterator().next();
                            mMessageAttachmentsOpener.saveAndDownloadAttachment(mMessageId, attachmentId);
                            mode.finish();
                        } else {
                            Toast.makeText(SingleMessageActivity.this,
                                R.string.singlemessage_save_not_available,
                                Toast.LENGTH_LONG).show();
                        }
                        return true;
                    }
                    case R.id.action_open_with: {
                        // Set contains only one element in this branch
                        long attachmentId = mAttachmentsAdapter.getSelectedItems().iterator().next();
                        mMessageAttachmentsOpener.saveAndOpenWithAttachment(mMessageId, attachmentId);
                        mode.finish();
                        return true;
                    }
                    case R.id.action_share: {
                        List<Long> attachmentsList = new ArrayList<>(mAttachmentsAdapter.getSelectedItems());
                        Collections.sort(attachmentsList);
                        mMessageAttachmentsOpener.saveAndShareAttachments(mMessageId, attachmentsList);
                        mode.finish();
                        return true;
                    }
                    default:
                        return false;
                }
            }
            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mAttachmentsAdapter.clearSelectedItems();
                mActionMode = null;
            }
        });
    }

    public void stopActionMode() {
        if (mAttachmentsAdapter != null && mActionMode != null) {
            mAttachmentsAdapter.clearSelectedItems();
            mActionMode.finish();
        }
    }

}
