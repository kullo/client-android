/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.app.Activity;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.kullo.android.R;
import net.kullo.android.application.CacheType;
import net.kullo.android.application.KulloApplication;
import net.kullo.android.kulloapi.CreateSessionResult;
import net.kullo.android.kulloapi.CreateSessionState;
import net.kullo.android.kulloapi.DialogMaker;
import net.kullo.android.kulloapi.KulloUtils;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.Debug;
import net.kullo.android.littlehelpers.Images;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.littlehelpers.StreamCopy;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.android.littlehelpers.UriHelpers;
import net.kullo.android.notifications.GcmConnector;
import net.kullo.android.observers.eventobservers.DraftAttachmentAddedEventObserver;
import net.kullo.android.observers.eventobservers.DraftAttachmentRemovedEventObserver;
import net.kullo.android.observers.eventobservers.DraftEventObserver;
import net.kullo.android.observers.listenerobservers.DraftAttachmentsAddListenerObserver;
import net.kullo.android.observers.listenerobservers.SyncerListenerObserver;
import net.kullo.android.screens.compose.DraftAttachmentOpener;
import net.kullo.android.screens.compose.DraftAttachmentsAdapter;
import net.kullo.android.ui.NonScrollingGridLayoutManager;
import net.kullo.android.ui.ScreenMetrics;
import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.LocalError;
import net.kullo.libkullo.api.NetworkError;
import net.kullo.libkullo.api.SyncProgress;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import io.github.dialogsforandroid.MaterialDialog;

public class ComposeActivity extends AppCompatActivity {
    private static final String TAG = "ComposeActivity";
    private static final int REQUEST_CODE_ATTACH_FILE = 1;

    private long mConversationId;

    private View mComposeFrame;
    private LinearLayout mComposeLayout;
    private View mComposeHeader;
    private TextView mComposeReceivers;
    private EditText mComposeText;
    private RecyclerView mAttachmentsList;
    @Nullable private MaterialDialog mProgressSync;
    private DraftEventObserver mDraftEventObserver;
    private DraftAttachmentsAddListenerObserver mDraftAttachmentsAddListenerObserver;
    private SyncerListenerObserver mSyncerListenerObserver;
    private DraftAttachmentsAdapter mDraftAttachmentsAdapter;
    private DraftAttachmentOpener mDraftAttachmentOpener;

    private boolean mSendingTriggered = false;
    @Nullable private ActionMode mDraftAttachmentsActionMode = null;

    // restore cursor position and selection when activity brought back from sleep
    @Nullable private Pair<Integer, Integer> mStoredTextSelection = null;

    private class PreparedAttachment {
        File tmpFile;
        String mimeType;

        PreparedAttachment(File tmpFile, String mimeType) {
            this.tmpFile = tmpFile;
            this.mimeType = mimeType;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final CreateSessionResult result = SessionConnector.get().createActivityWithSession(this);
        if (result.state == CreateSessionState.NO_CREDENTIALS) return;

        setContentView(R.layout.activity_compose);

        Ui.prepareActivityForTaskManager(this);

        Intent intent = getIntent();
        if (intent.hasExtra(KulloConstants.CONVERSATION_ID)) {
            mConversationId = intent.getLongExtra(KulloConstants.CONVERSATION_ID, -1);
            RuntimeAssertion.require(mConversationId != -1);
        } else if (intent.hasExtra(KulloConstants.CONVERSATION_RECIPIENT)) {
            String recipient = intent.getStringExtra(KulloConstants.CONVERSATION_RECIPIENT);
            mConversationId = SessionConnector.get().startConversationWithSingleRecipient(recipient);
        } else if (intent.getData().getScheme().equals("kullo")) {
            Log.d(TAG, Debug.getIntentDetails(intent));

            String addressString = intent.getData().getSchemeSpecificPart();
            if (KulloUtils.isValidKulloAddress(addressString)) {
                mConversationId = SessionConnector.get().startConversationWithSingleRecipient(addressString);
            }
        } else {
            Log.d(TAG, Debug.getIntentDetails(intent));
            throw new RuntimeException("Unexpected start of activity: No valid intent data found.");
        }

        Ui.setStatusBarColor(this);
        Ui.setupActionbar(this);
        setTitle(getString(R.string.activity_title_compose));
        setupUi();

        mDraftAttachmentOpener = new DraftAttachmentOpener(this);
        mDraftAttachmentOpener.registerSaveFinishedListenerObserver();
        mDraftAttachmentsAdapter = new DraftAttachmentsAdapter(this, mConversationId, mDraftAttachmentOpener);
        mDraftAttachmentsAdapter.onItemClickListener = new DraftAttachmentsAdapter.OnItemClickListener() {
            @Override
            public void onItemClicked(int position, long id) {
                if (mDraftAttachmentsAdapter.isSelectionActive()) {
                    selectDraftAttachment(id);
                } else {
                    mDraftAttachmentOpener.saveAndOpenAttachment(mConversationId, id);
                }
            }
        };
        mDraftAttachmentsAdapter.onItemLongClickListener = new DraftAttachmentsAdapter.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClicked(int position, long id) {
                selectDraftAttachment(id);
                return true;
            }
        };
        mAttachmentsList.setAdapter(mDraftAttachmentsAdapter);

        SessionConnector.get().addEventObserver(DraftAttachmentAddedEventObserver.class, new DraftAttachmentAddedEventObserver() {
            public void draftAttachmentAdded(long conversationId, long attachmentId) {
                mDraftAttachmentsAdapter.append(attachmentId);

                // call explicitly here because onLayoutChange() is not triggered when
                // going from 0 to 1 elements
                adaptLayoutToAttachmentsListSize();
            }
        });

        SessionConnector.get().addEventObserver(DraftAttachmentRemovedEventObserver.class, new DraftAttachmentRemovedEventObserver() {
            public void draftAttachmentRemoved(long conversationId, long attachmentId) {
                mDraftAttachmentsAdapter.remove(attachmentId);
            }
        });

        // resize edit window when attachments added or removed
        mAttachmentsList.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                adaptLayoutToAttachmentsListSize();
            }
        });

        registerSyncFinishedListenerObserver();

        if (result.state == CreateSessionState.CREATING) {
            RuntimeAssertion.require(result.task != null);
            result.task.waitUntilDone();
        }

        handleShares(intent);

        GcmConnector.get().ensureSessionHasTokenRegisteredAsync();
    }

    @Override
    protected void onStart() {
        super.onStart();

        mComposeReceivers.setText(SessionConnector.get().getConversationNameOrPlaceHolder(mConversationId));

        updateDraftTextFromStorage();

        registerObservers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        GcmConnector.get().removeAllNotifications(this);

        restoreTextSelection();
    }

    @Override
    protected void onPause() {
        super.onPause();

        storeTextSelection();
        saveDraft(false);
        Log.d(TAG, "Activity paused. Draft saved.");
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterObservers();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterSyncFinishedListenerObserver();
        mDraftAttachmentOpener.unregisterSaveFinishedListenerObserver();
    }

    private void selectDraftAttachment(long id) {
        mDraftAttachmentsAdapter.toggleSelectedItem(id);

        if (!mDraftAttachmentsAdapter.isSelectionActive()) {
            if (mDraftAttachmentsActionMode != null) {
                mDraftAttachmentsActionMode.finish();
            }
        } else {
            if (mDraftAttachmentsActionMode == null) setupDraftAttachmentsActionMode();
            final String title = String.format(
                    getResources().getString(R.string.actionmode_title_n_selected),
                    mDraftAttachmentsAdapter.getSelectedItemsCount());
            mDraftAttachmentsActionMode.setTitle(title);
        }
    }

    private void setupDraftAttachmentsActionMode() {
        mDraftAttachmentsActionMode = startActionMode(new ActionMode.Callback() {
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
                        List<Long> selectedDraftAttachmentsIdsCopy = new ArrayList<>(mDraftAttachmentsAdapter.getSelectedItems());
                        for (long attachmentId : selectedDraftAttachmentsIdsCopy) {
                            SessionConnector.get().removeDraftAttachment(mConversationId, attachmentId);
                        }
                        mode.finish();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mDraftAttachmentsAdapter.clearSelectedItems();
                mDraftAttachmentsActionMode = null;
            }
        });
    }

    private void setupUi() {
        mComposeFrame = findViewById(R.id.compose_frame);
        mComposeLayout = (LinearLayout) findViewById(R.id.compose_layout);
        mComposeHeader = findViewById(R.id.compose_header);
        mComposeReceivers = (TextView) findViewById(R.id.compose_receivers);
        mComposeText = (EditText) findViewById(R.id.compose_text);

        mAttachmentsList = (RecyclerView) findViewById(R.id.attachments_list);
        mAttachmentsList.setNestedScrollingEnabled(false);
        // Set dummy layout manager to avoid error log "E/RecyclerView: No adapter attached; skipping layout"
        final LinearLayoutManager dummyLayoutManager = new LinearLayoutManager(this);
        mAttachmentsList.setLayoutManager(dummyLayoutManager);

        // We need to wait for the RecyclerView layouting in order to have width available
        mAttachmentsList.post(new Runnable() {
            @Override
            public void run() {
                int columns = ScreenMetrics.getColumnsForComponent(mAttachmentsList, 100.0f);
                final NonScrollingGridLayoutManager glm = new NonScrollingGridLayoutManager(ComposeActivity.this, columns);
                mAttachmentsList.setLayoutManager(glm);
            }
        });
    }

    @UiThread
    private void handleShares(Intent intent) {
        RuntimeAssertion.require(SessionConnector.get().sessionAvailable());

        if (intent.hasExtra(KulloConstants.CONVERSATION_ADD_ATTACHMENTS)) {
            List<Uri> files = intent.getParcelableArrayListExtra(KulloConstants.CONVERSATION_ADD_ATTACHMENTS);
            RuntimeAssertion.require(files != null);

            copyNewAttachmentFilesToCacheAsync(files, new CopiedToCacheCallback() {
                @Override
                void onCopiedToCache(List<PreparedAttachment> copiedFiles) {
                    processFilesAsync(copiedFiles);
                }
            });
        } else if (intent.hasExtra(KulloConstants.CONVERSATION_ADD_TEXT)) {
            String text = intent.getStringExtra(KulloConstants.CONVERSATION_ADD_TEXT);
            RuntimeAssertion.require(text != null);

            String draftText = SessionConnector.get().getDraftText(mConversationId);
            if (!draftText.trim().isEmpty()) {
                draftText += "\n";
            }
            draftText += text;
            SessionConnector.get().setDraftText(mConversationId, draftText);
        }
    }

    @MainThread
    private void processFilesAsync(final List<PreparedAttachment> copiedFiles) {
        Deque<PreparedAttachment> files = new LinkedList<>(copiedFiles);
        processFilesAsync(files);
    }

    @MainThread
    private void processFilesAsync(final Deque<PreparedAttachment> copiedFiles) {
        int imagesCount = 0;
        for (PreparedAttachment attachment : copiedFiles) {
            if (Images.SCALABLE_TYPES.contains(attachment.mimeType)) {
                ++imagesCount;
            }
        }

        final int SIZE_L = 8;
        final int SIZE_M = 3;
        final int SIZE_S = 1;

        if (imagesCount > 0) {
            new MaterialDialog.Builder(this)
                    .title(imagesCount == 1
                            ? R.string.compose_scale_image_title
                            : R.string.compose_scale_images_title)
                    .items(
                            String.format(getString(R.string.compose_scale_option_xmegapixel), SIZE_L),
                            String.format(getString(R.string.compose_scale_option_xmegapixel), SIZE_M),
                            String.format(getString(R.string.compose_scale_option_xmegapixel), SIZE_S),
                            getString(R.string.compose_scale_option_unchanged)
                    )
                    .itemsCallback(new MaterialDialog.ListCallback() {
                        @Override
                        public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                            if (which < 3) {
                                int pixelLimit = -1;
                                switch (which) {
                                    case 0:
                                        pixelLimit = SIZE_L*Images.MEGA_PIXEL;
                                        break;
                                    case 1:
                                        pixelLimit = SIZE_M*Images.MEGA_PIXEL;
                                        break;
                                    case 2:
                                        pixelLimit = SIZE_S*Images.MEGA_PIXEL;
                                        break;
                                    default:
                                        RuntimeAssertion.fail("Unhandled value");
                                }
                                scaleDownImagesInFilesList(copiedFiles, pixelLimit, new Runnable() {
                                    @Override
                                    public void run() {
                                        copyNextFileToDatabase(copiedFiles, null);
                                    }
                                });
                            } else {
                                copyNextFileToDatabase(copiedFiles, null);
                            }
                        }
                    })
                    .dismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            // dismissed
                        }
                    })
                    .show();
        } else {
            copyNextFileToDatabase(copiedFiles, null);
        }
    }

    private void scaleDownImagesInFilesList(
            @NonNull final Collection<PreparedAttachment> copiedFiles,
            final int pixelLimit,
            @Nullable final Runnable allDoneCallback) {
        int count = 0;
        for (PreparedAttachment attachment : copiedFiles) {
            if (Images.SCALABLE_TYPES.contains(attachment.mimeType)) {
                count += 1;
            }
        }
        final int imagesCount = count;
        final boolean showDeterminateProgress = (imagesCount >= 3);

        new AsyncTask<Void, Void, Void>() {
            MaterialDialog mScalingDialog;

            @Override
            protected void onPreExecute() {

                mScalingDialog = new MaterialDialog.Builder(ComposeActivity.this)
                        .title(R.string.compose_scale_progress_title)
                        .content(R.string.compose_scale_progress_description)
                        .progress(!showDeterminateProgress, imagesCount, true)
                        .cancelable(false)
                        .build();
                mScalingDialog.show();
            }

            @Override
            protected Void doInBackground(Void... voids) {
                int doneCount = 0;
                for (PreparedAttachment attachment : copiedFiles) {
                    if (Images.SCALABLE_TYPES.contains(attachment.mimeType)) {
                        Log.d(TAG, "Found file type that can be scaled down: " + attachment.tmpFile);
                        Images.scaleDownInplace(attachment.tmpFile, pixelLimit);

                        doneCount += 1;

                        // setProgress must not be set in indeterminate mode
                        // https://github.com/afollestad/material-dialogs/issues/1235
                        // setProgress is not thread-safe
                        // https://github.com/afollestad/material-dialogs/pull/1238
                        if (showDeterminateProgress) {
                            final int finalDoneCount = doneCount;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mScalingDialog.setProgress(finalDoneCount);
                                }
                            });
                        }
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                mScalingDialog.dismiss();
                if (allDoneCallback != null) allDoneCallback.run();
            }
        }.execute();
    }

    // Method calls itself until all files are copied and than calls the callback once
    @MainThread
    void copyNextFileToDatabase(
            @NonNull final Deque<PreparedAttachment> copiedFiles,
            @Nullable final Runnable allDoneCallback) {
        if (copiedFiles.isEmpty()) {
            if (allDoneCallback != null) allDoneCallback.run();
            return;
        }

        final PreparedAttachment attachment = copiedFiles.pollFirst();
        final String path = attachment.tmpFile.getAbsolutePath();
        final String mimeType = attachment.mimeType;
        SessionConnector.get().addAttachmentToDraft(mConversationId, path, mimeType, new SessionConnector.AddAttachmentToDraftCallback() {
            @Override
            public void run(boolean success) {
                // ignore errors and continue

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        copyNextFileToDatabase(copiedFiles, allDoneCallback);
                    }
                });
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // onWindowFocusChanged() is called late enough for this
        // http://stackoverflow.com/a/6560726/2013738
        adaptLayoutToAttachmentsListSize();
    }

    protected void adaptLayoutToAttachmentsListSize() {
        // Make message input box fill entire available space such that the user can focus it by
        // touching the whole space. This must be done after we set recipients and know the
        // final header size.
        // Those updates must converge to avoid endless recursion
        int frameHeightPx = mComposeFrame.getHeight();
        int headerHeightPx = mComposeHeader.getHeight();
        int dividerHeightPx = mComposeLayout.getDividerDrawable().getIntrinsicHeight();

        // Attachments list
        if (mDraftAttachmentsAdapter.getItemCount() == 0) {
            if (mAttachmentsList.getVisibility() != View.GONE) {
                mAttachmentsList.setVisibility(View.GONE);
            }
        } else {
            if (mAttachmentsList.getVisibility() != View.VISIBLE) {
                mAttachmentsList.setVisibility(View.VISIBLE);
            }
        }

        // Text box
        int attachmentListHeight = 0;
        int extraDividerHeight = 0;
        if (mDraftAttachmentsAdapter.getItemCount() > 0) {
            attachmentListHeight = mAttachmentsList.getHeight()
                + ((LinearLayout.LayoutParams) mAttachmentsList.getLayoutParams()).topMargin
                + ((LinearLayout.LayoutParams) mAttachmentsList.getLayoutParams()).bottomMargin;
            extraDividerHeight = dividerHeightPx;
            if (attachmentListHeight > frameHeightPx / 2) {
                // prevent attachment list to take over the whole view (user will have to scroll down past this point)
                attachmentListHeight = frameHeightPx / 2;
            }
        }

        //Log.d(TAG, "frame: " + frameHeight + ", header: " + headerHeight + ", divider: " + dividerHeight + ", attachments: " + attachmentListHeight + " + " + extraDividerHeight);

        int newMinimumHeight = frameHeightPx - headerHeightPx - dividerHeightPx - attachmentListHeight - extraDividerHeight;
        if (mComposeText.getMinimumHeight() != newMinimumHeight) {
            mComposeText.setMinimumHeight(newMinimumHeight);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ATTACH_FILE) {
            if (resultCode == Activity.RESULT_OK) {
                RuntimeAssertion.require(data != null);

                final List<Uri> selectedFileUris = new ArrayList<>();
                if (data.getClipData() != null) {
                    final ClipData clipdata = data.getClipData();
                    for (int i = 0; i < clipdata.getItemCount(); ++i) {
                        final Uri u = clipdata.getItemAt(i).getUri();
                        selectedFileUris.add(u);
                    }
                } else {
                    final Uri selectedFileUri = data.getData();
                    RuntimeAssertion.require(selectedFileUri != null);
                    selectedFileUris.add(selectedFileUri);
                }
                Log.d(TAG, "Attachment sources: " + selectedFileUris);

                copyNewAttachmentFilesToCacheAsync(selectedFileUris, new CopiedToCacheCallback() {
                    @Override
                    void onCopiedToCache(List<PreparedAttachment> copiedFiles) {
                        processFilesAsync(copiedFiles);
                    }
                });
            } else if (resultCode == Activity.RESULT_CANCELED){
                Toast.makeText(this, R.string.select_file_canceled, Toast.LENGTH_SHORT).show();
            } else {
                RuntimeAssertion.fail("Unknown result code");
            }
        } else {
            RuntimeAssertion.fail("Unknown request code: " + requestCode);
        }
    }

    abstract class CopiedToCacheCallback {
        abstract void onCopiedToCache(List<PreparedAttachment> selectedFileUris);
    }

    @MainThread
    private void copyNewAttachmentFilesToCacheAsync(
        @NonNull final List<Uri> selectedFileUris,
        @NonNull final CopiedToCacheCallback callback
    ) {
        new AsyncTask<Void, Void, List<PreparedAttachment>>() {
            boolean mError = false;

            @Override
            protected List<PreparedAttachment> doInBackground(Void... params) {
                final List<PreparedAttachment> out = new LinkedList<>();
                for (final Uri selectedFileUri : selectedFileUris) {
                    final PreparedAttachment a = prepareAttachment(selectedFileUri);
                    if (a != null) {
                        out.add(a);
                    } else {
                        mError = true;
                    }
                }

                return out;
            }

            @Override
            protected void onPostExecute(List<PreparedAttachment> preparedAttachments) {
                if (mError) {
                    Toast.makeText(ComposeActivity.this, R.string.compose_error_loading_file,
                        Toast.LENGTH_SHORT).show();
                }

                callback.onCopiedToCache(preparedAttachments);
            }
        }.execute();
    }

    private void updateDraftTextFromStorage() {
        //set old draft text, if there is any
        final String draftText = SessionConnector.get().getDraftText(mConversationId);
        if (!draftText.isEmpty()) {
            mComposeText.setText(draftText);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: // main back button
                Log.d(TAG, "Main back button clicked");
                setResult(RESULT_CANCELED);
                finish(); // this will cause onPause, which saves the draft
                return true;
            case R.id.action_add_attachment:
                startFileSelectionActivity();
                return true;
            case R.id.action_send:
                send();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.appbar_menu_compose, menu);
        return true;
    }

    private boolean hasContent() {
        String message = mComposeText.getText().toString();
        return !message.isEmpty() || mDraftAttachmentsAdapter.getItemCount() > 0;
    }

    private void send() {
        if (hasContent()) {
            mSendingTriggered = true;

            mProgressSync = new MaterialDialog.Builder(this)
                    .title(R.string.progress_send)
                    .content(R.string.please_wait)
                    .progress(false, 100)
                    .cancelable(false)
                    .show();

            saveDraft(true);
            SessionConnector.get().sendMessages();
        }
        // otherwise ignore click
    }

    private void saveDraft(boolean sendDraft) {
        String message = mComposeText.getText().toString();
        if (sendDraft) {
            SessionConnector.get().saveDraftForSending(mConversationId, message);
        } else {
            SessionConnector.get().setDraftText(mConversationId, message);
        }
    }

    private void registerSyncFinishedListenerObserver() {
        mSyncerListenerObserver = new SyncerListenerObserver() {
            @Override
            public void started() {
            }

            @Override
            public void draftAttachmentsTooBig(long convId) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //TODO error message draftAttachmentsTooBig
                    }
                });
            }

            @Override
            public void progressed(final SyncProgress progress) {
                if (progress.getOutgoingMessagesTotalBytes() != 0) {
                    // only update progress bar when information is available
                    // to avoid a 100% -> 0% step when a download sync is
                    // processed before the dialog is closed
                    final int percentages = Math.min(100,
                            (int) ((100 * progress.getOutgoingMessagesUploadedBytes()) / progress.getOutgoingMessagesTotalBytes()));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mProgressSync != null) {
                                mProgressSync.setProgress(percentages);
                            }
                        }
                    });
                }
            }

            @Override
            public void finished() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mSendingTriggered) {
                            if (mProgressSync != null) {
                                mProgressSync.dismiss();
                            }

                            setResult(RESULT_OK);
                            finish();
                        }
                    }
                });
            }

            @Override
            public void error(final NetworkError error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mProgressSync != null) {
                            mProgressSync.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                public void onDismiss(DialogInterface dialog) {
                                    DialogMaker.makeForNetworkError(ComposeActivity.this, error).show();
                                }
                            });

                            mProgressSync.dismiss();
                        }
                    }
                });
            }
        };
        SessionConnector.get().addListenerObserver(SyncerListenerObserver.class,
                mSyncerListenerObserver);
    }

    private void unregisterSyncFinishedListenerObserver() {
        SessionConnector.get().removeListenerObserver(SyncerListenerObserver.class,
                mSyncerListenerObserver);
    }

    private void storeTextSelection() {
        mStoredTextSelection = new Pair<>(
                mComposeText.getSelectionStart(),
                mComposeText.getSelectionEnd());
    }

    private void restoreTextSelection() {
        if (mStoredTextSelection != null) {
            int textLength = mComposeText.getText().length();
            mComposeText.setSelection(
                    Math.min(textLength, mStoredTextSelection.first),
                    Math.min(textLength, mStoredTextSelection.second));
        }
    }


    // ATTACHMENTS

    private void startFileSelectionActivity() {
        final Intent intent = new Intent();
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        startActivityForResult(intent, REQUEST_CODE_ATTACH_FILE);
    }

    @NonNull
    private static String mimeTypeOrFallback(@Nullable final String mimeType) {
        if (mimeType != null) return mimeType;
        else return "application/octet-stream";
    }

    @WorkerThread
    @Nullable
    private PreparedAttachment prepareAttachment(@NonNull final Uri selectedFileUri) {
        final String guessedMimeType;
        final String scheme = selectedFileUri.getScheme();
        switch (scheme) {
            case "file":
                guessedMimeType = URLConnection.guessContentTypeFromName(selectedFileUri.toString());
                break;
            case "content":
                guessedMimeType = getContentResolver().getType(selectedFileUri);
                break;
            default:
                RuntimeAssertion.fail("Unrecognized scheme: " + scheme);
                return null;
        }
        final String mimeType = mimeTypeOrFallback(guessedMimeType);
        final File tmpFile = doCopyAttachmentToCache(selectedFileUri);
        if (tmpFile != null) {
            return new PreparedAttachment(tmpFile, mimeType);
        } else {
            return null;
        }
    }

    @WorkerThread
    @Nullable
    private File doCopyAttachmentToCache(@NonNull final Uri selectedFileUri) {
        String tmpFilename = UriHelpers.getFilename(this, selectedFileUri, "tmp.tmp");
        File cacheDir = ((KulloApplication) getApplication()).cacheDir(CacheType.AddAttachment, null);
        final File tmpFile = new File(cacheDir, tmpFilename);
        Log.d(TAG, "Tmp file path before adding to DB: " + tmpFile);

        if (StreamCopy.copyToPath(this, selectedFileUri, tmpFile.getAbsolutePath())) {
            return tmpFile;
        } else {
            Log.e(TAG, "Could not copy " + selectedFileUri + " to " + tmpFile);
            return null;
        }
    }

    private void registerObservers() {
        mDraftEventObserver = new DraftEventObserver() {
            @Override
            public void draftStateChanged(long conversationId) {
                Log.w(TAG, "möp");
            }

            @Override
            public void draftTextChanged(long conversationId) {
                mComposeText.setText(SessionConnector.get().getDraftText(conversationId));
            }
        };
        SessionConnector.get().addEventObserver(DraftEventObserver.class, mDraftEventObserver);

        mDraftAttachmentsAddListenerObserver = new DraftAttachmentsAddListenerObserver() {
            @Override
            public void finished(long convId, long attId, String path) {

            }

            @Override
            public void error(final LocalError error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String text;
                        switch (error) {
                            case FILETOOBIG:
                                text = getString(R.string.compose_add_attachment_error_toobig);
                                break;
                            case FILESYSTEM:
                                text = getString(R.string.compose_add_attachment_error_filesystem);
                                break;
                            case UNKNOWN:
                                text = getString(R.string.compose_add_attachment_error_unknown);
                                break;
                            default:
                                throw new AssertionError("Unhandled enum value");
                        }
                        Toast.makeText(ComposeActivity.this, text, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
        SessionConnector.get().addListenerObserver(DraftAttachmentsAddListenerObserver.class, mDraftAttachmentsAddListenerObserver);
    }

    private void unregisterObservers() {
        SessionConnector.get().removeEventObserver(DraftEventObserver.class, mDraftEventObserver);
        SessionConnector.get().removeListenerObserver(DraftAttachmentsAddListenerObserver.class, mDraftAttachmentsAddListenerObserver);
    }
}
