/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import net.kullo.android.R;
import net.kullo.android.application.KulloApplication;
import net.kullo.android.kulloapi.KulloConnector;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.android.observers.eventobservers.MessageAttachmentsDownloadedChangedEventObserver;
import net.kullo.android.observers.listenerobservers.DownloadAttachmentsForMessageListenerObserver;
import net.kullo.android.screens.conversationslist.RecyclerItemClickListener;
import net.kullo.android.screens.messageslist.AttachmentsAdapter;
import net.kullo.android.screens.messageslist.MessageAttachmentsOpener;
import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.AsyncTask;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class SingleMessageActivity extends AppCompatActivity {
    private long mMessageId;

    private MessageAttachmentsOpener mMessageAttachmentsOpener;
    private DownloadAttachmentsForMessageListenerObserver mDownloadAttachmentsFinishedObserver;
    private MessageAttachmentsDownloadedChangedEventObserver mMessageAttachmentsDownloadedChangedEventObserver;

    public static final DateTimeZone LOCAL_TIME_ZONE = DateTimeZone.getDefault();
    protected DateTimeFormatter mFormatterClock;
    protected DateTimeFormatter mFormatterCalendarDate;

    // Views
    protected CircleImageView mCircleImageView;
    protected TextView mMessageDateTextView;
    protected TextView mSenderNameTextView;
    protected TextView mSenderOrganizationTextView;
    protected TextView mMessageContentTextView;
    protected RecyclerView mAttachmentsList;
    protected Button mDownloadButton;
    protected View mFooterDivider;
    protected Button mFooterButton;
    protected TextView mFooterTextView;

    private MaterialDialog mShowSettingsDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AsyncTask task = KulloConnector.get().createActivityWithSession(this);

        setContentView(R.layout.activity_single_message);

        Intent intent = getIntent();
        mMessageId = intent.getLongExtra(KulloConstants.MESSAGE_ID, -1);
        RuntimeAssertion.require(mMessageId != -1);

        Ui.setupActionbar(this);
        Ui.setColorStatusBarArrangeHeader(this);

        mFormatterCalendarDate = ((KulloApplication) getApplication()).getShortDateFormatter();
        mFormatterClock = ((KulloApplication) getApplication()).getShortTimeFormatter();

        mDownloadAttachmentsFinishedObserver = new DownloadAttachmentsForMessageListenerObserver() {
            @Override
            public void finished() {
                showAttachmentsListIfAvailable();
            }

            @Override
            public void error(final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new MaterialDialog.Builder(SingleMessageActivity.this)
                                .title(R.string.error_title)
                                .content(error)
                                .neutralText(R.string.ok)
                                .cancelable(false)
                                .show();
                    }
                });
            }
        };
        KulloConnector.get().addListenerObserver(DownloadAttachmentsForMessageListenerObserver.class,
                mDownloadAttachmentsFinishedObserver);

        mMessageAttachmentsDownloadedChangedEventObserver = new MessageAttachmentsDownloadedChangedEventObserver() {
            @Override
            public void messageAttachmentsDownloadedChanged(final long mMessageId) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showAttachmentsListIfAvailable();
                    }
                });
            }
        };
        KulloConnector.get().addEventObserver(MessageAttachmentsDownloadedChangedEventObserver.class,
                mMessageAttachmentsDownloadedChangedEventObserver);

        mMessageAttachmentsOpener = new MessageAttachmentsOpener(this);
        mMessageAttachmentsOpener.registerSaveFinishedListenerObserver();

        mCircleImageView = (CircleImageView) findViewById(R.id.sender_avatar);
        mMessageDateTextView = (TextView) findViewById(R.id.message_date);
        mSenderNameTextView = (TextView) findViewById(R.id.sender_name);
        mSenderOrganizationTextView = (TextView) findViewById(R.id.sender_company);
        mMessageContentTextView = (TextView) findViewById(R.id.message_content);
        mAttachmentsList = (RecyclerView) findViewById(R.id.attachments_list);
        mDownloadButton = (Button) findViewById(R.id.download_button);
        mFooterDivider = findViewById(R.id.footer_divider);
        mFooterButton = (Button) findViewById(R.id.footer_button);
        mFooterTextView = (TextView) findViewById(R.id.footer_text);

        if (task != null) task.waitUntilDone();
    }

    @Override
    protected void onStart() {
        super.onStart();

        KulloConnector.get().setMessageRead(mMessageId);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        populateMessageFields();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMessageAttachmentsOpener.unregisterSaveFinishedListenerObserver();
        KulloConnector.get().removeEventObserver(MessageAttachmentsDownloadedChangedEventObserver.class,
                mMessageAttachmentsDownloadedChangedEventObserver);
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
            case R.id.action_write:
                if (KulloConnector.get().userSettingsAreValidForSync()) {
                    long conversationId = KulloConnector.get().getMessageConversation(mMessageId);
                    Intent intent = new Intent(this, ComposeActivity.class);
                    intent.putExtra(KulloConstants.CONVERSATION_ID, conversationId);
                    startActivityForResult(intent, KulloConstants.REQUEST_CODE_NEW_MESSAGE);
                } else {
                    showDialogToShowUserSettingsForCompletion();
                }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.appbar_menu_single_message, menu);
        return true;
    }

    public void populateMessageFields() {
        final DateTime dateReceived = KulloConnector.get().getMessageDateReceived(mMessageId);
        final String messageText = KulloConnector.get().getMessageText(mMessageId);
        final String messageTextCompressed = messageText.replaceAll("\\s+", " ");
        final String messageFooter = KulloConnector.get().getMessageFooter(mMessageId);
        final Bitmap senderAvatar = KulloConnector.get().getSenderAvatar(this, mMessageId);
        final String senderName = KulloConnector.get().getSenderName(mMessageId);
        final String senderOrganization = KulloConnector.get().getSenderOrganization(mMessageId);

        mCircleImageView.setImageBitmap(senderAvatar);
        mMessageDateTextView.setText(getDateText(dateReceived));
        mSenderNameTextView.setText(senderName);
        mSenderOrganizationTextView.setText(senderOrganization);

        setTitle(senderName);

        setMessageText(messageText, messageTextCompressed);

        showAttachmentsListIfAvailable();

        if (messageFooter.isEmpty()) {
            hideFooterContainer();
        } else {
            mFooterTextView.setText(messageFooter);
            showFooterContainer(false);
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
                        startActivity(new Intent(SingleMessageActivity.this, SettingsActivity.class));
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        mShowSettingsDialog.dismiss();
                    }
                })
                .show();
    }

    private void showAttachmentsListIfAvailable() {
        final ArrayList<Long> attachmentIDs = KulloConnector.get().getMessageAttachmentsIds(mMessageId);
        final boolean attachmentsDownloaded = KulloConnector.get().getMessageAttachmentsDownloaded(mMessageId);

        if (attachmentIDs == null || attachmentIDs.size() == 0) {
            setAttachmentsListVisibility(View.GONE);
            return;
        }

        setAttachmentsListVisibility(View.VISIBLE);

        LinearLayoutManager layoutManager = new org.solovyev.android.views.llm.LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mAttachmentsList.setLayoutManager(layoutManager);

        final AttachmentsAdapter attachmentsAdapter = new AttachmentsAdapter(SingleMessageActivity.this, mMessageId, attachmentsDownloaded);
        mAttachmentsList.setAdapter(attachmentsAdapter);

        if (attachmentsDownloaded) {
            mAttachmentsList.setEnabled(true);
            mDownloadButton.setVisibility(View.GONE);
            mAttachmentsList.addOnItemTouchListener(new RecyclerItemClickListener(SingleMessageActivity.this, mAttachmentsList,
                new RecyclerItemClickListener.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    long attachmentId = attachmentsAdapter.getItem(position);
                    mMessageAttachmentsOpener.saveAndOpenAttachment(mMessageId, attachmentId);
                }
                @Override
                public void onItemLongPress(View view, int position) {}
            }));
        } else {
            mAttachmentsList.setEnabled(false);
            mDownloadButton.setVisibility(View.VISIBLE);
            mDownloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    KulloConnector.get().downloadAttachments(mMessageId);
                }
            });
        }
    }

    private void setAttachmentsListVisibility(int visibility) {
        mAttachmentsList.setVisibility(visibility);
        mDownloadButton.setVisibility(visibility);
    }

    @NonNull
    private String getDateText(DateTime dateReceived) {
        LocalDateTime localDateReceived = new LocalDateTime(dateReceived, LOCAL_TIME_ZONE);

        String dateString;
        if(localDateReceived.toLocalDate().equals(new LocalDate())) {
            dateString = localDateReceived.toString(mFormatterClock);
        } else if(localDateReceived.toLocalDate().equals((new LocalDate()).minusDays(1))) {
            dateString = SingleMessageActivity.this.getResources().getString(R.string.yesterday);
        } else {
            dateString = localDateReceived.toString(mFormatterCalendarDate);
        }

        return dateString;
    }

    private void setMessageText(String messageText, String messageTextCompressed) {
        mMessageContentTextView.setMaxLines(Integer.MAX_VALUE);
        mMessageContentTextView.setAutoLinkMask(Linkify.WEB_URLS);
        mMessageContentTextView.setText(messageText);
    }

    private void showFooterContainer(Boolean showFooter) {
        mFooterDivider.setVisibility(View.VISIBLE);
        mFooterButton.setVisibility(View.VISIBLE);

        if (!showFooter) {
            mFooterTextView.setVisibility(View.GONE);
            mFooterButton.setText(SingleMessageActivity.this.getResources().getString(R.string.show_footer));

            mFooterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showFooterContainer(true);
                }
            });
        } else {
            mFooterTextView.setVisibility(View.VISIBLE);
            mFooterButton.setText(SingleMessageActivity.this.getResources().getString(R.string.hide_footer));

            mFooterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showFooterContainer(false);
                }
            });
        }
    }

    private void hideFooterContainer() {
        mFooterDivider.setVisibility(View.GONE);
        mFooterTextView.setVisibility(View.GONE);
        mFooterButton.setVisibility(View.GONE);
    }

}
