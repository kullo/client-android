/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
import net.kullo.libkullo.api.NetworkError;
import net.kullo.libkullo.api.SyncProgress;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import io.github.dialogsforandroid.MaterialDialog;

public class SingleMessageActivity extends AppCompatActivity {
    @SuppressWarnings("unused") private static final String TAG = "SingleMessageActivity";
    private long mMessageId;

    private MessageAttachmentsOpener mMessageAttachmentsOpener;
    private SyncerListenerObserver mDownloadAttachmentsFinishedObserver;
    private MessageAttachmentsDownloadedChangedEventObserver mMessageAttachmentsDownloadedChangedEventObserver;

    private static final DateTimeZone LOCAL_TIME_ZONE = DateTimeZone.getDefault();
    private DateTimeFormatter mFormatterDate;

    private AttachmentsAdapter mAttachmentsAdapter = null;
    private RecyclerItemClickListener mAttachmentsClickListener = null;
    @Nullable private ActionMode mActionMode = null;

    // Views
    private View mOptionalPaddingElement;
    private CircleImageView mCircleImageView;
    private TextView mMessageDateTextView;
    private TextView mSenderNameTextView;
    private TextView mSenderOrganizationTextView;
    private TextView mMessageContentTextView;
    private RecyclerView mAttachmentsList;
    private Button mDownloadButton;
    private View mFooterContainer;
    private ImageButton mToggleFooterButton;
    private TextView mFooterTextView;

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
        Ui.setColorStatusBarArrangeHeader(this);

        mFormatterDate = ((KulloApplication) getApplication()).getFullDateTimeFormatter();
        mMessageAttachmentsOpener = new MessageAttachmentsOpener(this);

        registerObservers();
        setupUi();

        if (result.state == CreateSessionState.CREATING) {
            RuntimeAssertion.require(result.task != null);
            result.task.waitUntilDone();
        }

        GcmConnector.get().fetchAndRegisterToken(this);

        populateMessageFields();
    }

    private void setupUi() {
        mCircleImageView = (CircleImageView) findViewById(R.id.sender_avatar);
        mMessageDateTextView = (TextView) findViewById(R.id.message_date);
        mSenderNameTextView = (TextView) findViewById(R.id.sender_name);
        mSenderOrganizationTextView = (TextView) findViewById(R.id.sender_company);
        mMessageContentTextView = (TextView) findViewById(R.id.message_content);
        mAttachmentsList = (RecyclerView) findViewById(R.id.attachments_list);
        mDownloadButton = (Button) findViewById(R.id.download_button);
        mOptionalPaddingElement = findViewById(R.id.optional_padding_element);
        mFooterContainer = findViewById(R.id.footer_container);
        mToggleFooterButton = (ImageButton) findViewById(R.id.footer_button);
        mFooterTextView = (TextView) findViewById(R.id.footer_text);

        mAttachmentsList.setNestedScrollingEnabled(false);
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
        clearSelection();
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
            public void progressed(SyncProgress progress) {
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
            @Override
            public void messageAttachmentsDownloadedChanged(final long mMessageId) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        reloadAttachmentsList();
                    }
                });
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
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_write:
                if (SessionConnector.get().userSettingsAreValidForSync()) {
                    long conversationId = SessionConnector.get().getMessageConversation(mMessageId);
                    Intent intent = new Intent(this, ComposeActivity.class);
                    intent.putExtra(KulloConstants.CONVERSATION_ID, conversationId);
                    startActivityForResult(intent, KulloConstants.REQUEST_CODE_NEW_MESSAGE);
                } else {
                    showDialogToShowUserSettingsForCompletion();
                }
                return true;
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
        final String senderName = SessionConnector.get().getSenderName(mMessageId);
        final String senderOrganization = SessionConnector.get().getSenderOrganization(mMessageId);

        mCircleImageView.setImageBitmap(senderAvatar);
        mMessageDateTextView.setText(getDateText(dateReceived));
        mSenderNameTextView.setText(senderName);
        mSenderOrganizationTextView.setText(senderOrganization);

        setTitle(senderName);

        mMessageContentTextView.setMaxLines(Integer.MAX_VALUE);
        final Spannable content = TextViewContent.getSpannableFromHtml(messageTextAsHtml, new TextViewContent.LinkClickedListener() {
            @Override
            protected void onClicked(Uri target) {
                startActivity(new Intent(Intent.ACTION_VIEW, target));
            }
        });
        mMessageContentTextView.setText(content);
        mMessageContentTextView.setMovementMethod(LinkMovementMethod.getInstance()); // make links clickable

        reloadAttachmentsList();

        if (messageFooter.isEmpty()) {
            hideFooterContainer();
        } else {
            mFooterTextView.setText(messageFooter);
            showFooterContainer(false);
        }
    }

    private void showDialogToShowUserSettingsForCompletion() {
        final MaterialDialog showSettingsDialog = CommonDialogs.buildShowSettingsDialog(this);
        showSettingsDialog.show();
    }

    private void reloadAttachmentsList() {
        final ArrayList<Long> attachmentIds = SessionConnector.get().getMessageAttachmentsIds(mMessageId);
        final boolean attachmentsDownloaded = SessionConnector.get().getMessageAttachmentsDownloaded(mMessageId);

        if (attachmentIds == null || attachmentIds.size() == 0) {
            setAttachmentsListVisibility(View.GONE);
            return;
        }

        setAttachmentsListVisibility(View.VISIBLE);

        mAttachmentsAdapter = new AttachmentsAdapter(SingleMessageActivity.this, mMessageId, attachmentsDownloaded);
        mAttachmentsList.setAdapter(mAttachmentsAdapter);

        if (attachmentsDownloaded) {
            mAttachmentsList.setEnabled(true);
            mDownloadButton.setVisibility(View.GONE);
            if (mAttachmentsClickListener == null) {
                mAttachmentsClickListener = new RecyclerItemClickListener(mAttachmentsList) {
                    @Override
                    public void onItemClick(View view, int position) {
                        long attachmentId = mAttachmentsAdapter.getItem(position);
                        if (mAttachmentsAdapter.isSelectionActive()) {
                            toggleAttachmentSelection(attachmentId);
                        } else {
                            mMessageAttachmentsOpener.saveAndOpenAttachment(mMessageId, attachmentId);
                        }
                    }

                    @Override
                    public void onItemLongPress(View view, int position) {
                        long attachmentId = mAttachmentsAdapter.getItem(position);
                        toggleAttachmentSelection(attachmentId);
                    }
                };
                mAttachmentsList.addOnItemTouchListener(mAttachmentsClickListener);
            }
        } else {
            mAttachmentsList.setEnabled(false);
            mDownloadButton.setVisibility(View.VISIBLE);
            mDownloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SessionConnector.get().downloadAttachments(mMessageId);
                }
            });
        }

        // Set dummy layout manager to avoid error log "E/RecyclerView: No adapter attached; skipping layout"
        LinearLayoutManager dummyLayoutManager = new LinearLayoutManager(this);
        mAttachmentsList.setLayoutManager(dummyLayoutManager);

        // We need to wait for the RecyclerView layouting in order to have width available
        mAttachmentsList.post(new Runnable() {
            @Override
            public void run() {
                int columns = ScreenMetrics.getColumnsForComponent(mAttachmentsList, 100.0f);
                final NonScrollingGridLayoutManager glm = new NonScrollingGridLayoutManager(SingleMessageActivity.this, columns);
                mAttachmentsList.setLayoutManager(glm);
            }
        });
    }

    private void setAttachmentsListVisibility(int visibility) {
        mAttachmentsList.setVisibility(visibility);
        mDownloadButton.setVisibility(visibility);
    }

    @NonNull
    private String getDateText(DateTime dateReceived) {
        LocalDateTime localDateReceived = new LocalDateTime(dateReceived, LOCAL_TIME_ZONE);

        return localDateReceived.toString(mFormatterDate);
    }

    private void showFooterContainer(boolean footerExpanded) {
        mOptionalPaddingElement.setVisibility(View.GONE);
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
        mOptionalPaddingElement.setVisibility(View.VISIBLE);
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
                getResources().getString(R.string.title_n_selected),
                mAttachmentsAdapter.getSelectedItemsCount());
            mActionMode.setTitle(title);
            boolean singleItemActions = mAttachmentsAdapter.getSelectedItemsCount() == 1;
            mActionMode.getMenu().findItem(R.id.action_open_with)
                .setVisible(singleItemActions);
            mActionMode.getMenu().findItem(R.id.action_save_as)
                .setVisible(singleItemActions && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT);
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
                     case R.id.action_save_as:
                         mMessageAttachmentsOpener.saveAndDownloadAttachment(mMessageId, mAttachmentsAdapter.getSelectedItems().get(0));
                         mode.finish();
                         return true;
                    case R.id.action_open_with:
                        mMessageAttachmentsOpener.saveAndOpenWithAttachment(mMessageId, mAttachmentsAdapter.getSelectedItems().get(0));
                        mode.finish();
                        return true;
                    case R.id.action_share:
                        mMessageAttachmentsOpener.saveAndShareAttachments(mMessageId, mAttachmentsAdapter.getSelectedItems());
                        mode.finish();
                        return true;
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

    public void clearSelection() {
        if (mAttachmentsAdapter != null && mActionMode != null) {
            mAttachmentsAdapter.clearSelectedItems();
            mActionMode.finish();
        }
    }

}
