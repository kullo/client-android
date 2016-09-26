/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import net.kullo.android.R;
import net.kullo.android.kulloapi.CreateSessionResult;
import net.kullo.android.kulloapi.CreateSessionState;
import net.kullo.android.kulloapi.DialogMaker;
import net.kullo.android.kulloapi.KulloUtils;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.Debug;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.littlehelpers.NonScrollingLinearLayoutManager;
import net.kullo.android.littlehelpers.StreamCopy;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.android.notifications.GcmConnector;
import net.kullo.android.observers.eventobservers.DraftAttachmentAddedEventObserver;
import net.kullo.android.observers.eventobservers.DraftAttachmentRemovedEventObserver;
import net.kullo.android.observers.eventobservers.DraftEventObserver;
import net.kullo.android.observers.listenerobservers.SyncerListenerObserver;
import net.kullo.android.screens.compose.DraftAttachmentOpener;
import net.kullo.android.screens.compose.DraftAttachmentsAdapter;
import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.AsyncTask;
import net.kullo.libkullo.api.NetworkError;
import net.kullo.libkullo.api.SyncProgress;

import java.io.File;
import java.net.URLConnection;

public class ComposeActivity extends AppCompatActivity {
    private static final String TAG = "ComposeActivity";
    private static final int REQUEST_CODE_ATTACH_FILE = 1;

    private long mConversationId;
    private EditText mNewMessageText;
    private TextView mNewMessageReceivers;
    private RecyclerView mDraftAttachmentsList;
    private MaterialDialog mProgressSync;
    private DraftEventObserver mDraftEventObserver;
    private SyncerListenerObserver mSyncerListenerObserver;
    private DraftAttachmentsAdapter mDraftAttachmentsAdapter;
    private DraftAttachmentOpener mDraftAttachmentOpener;

    // Android API < 16 hack because View.getMinimumHeight is missing
    private int currentMessageTextInputMinimumHeight = -1;

    private Pair<Integer, Integer> mStoredSelection = null;

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

        Ui.setColorStatusBarArrangeHeader(this);
        Ui.setupActionbar(this);
        setTitle(getString(R.string.activity_title_compose));

        mNewMessageText = (EditText) findViewById(R.id.new_message_text);
        mNewMessageReceivers = (TextView) findViewById(R.id.new_message_receivers);

        mDraftAttachmentsList = (RecyclerView) findViewById(R.id.draft_attachments_list);
        mDraftAttachmentsList.setLayoutManager(new NonScrollingLinearLayoutManager(this));

        mDraftAttachmentOpener = new DraftAttachmentOpener(this);
        mDraftAttachmentOpener.registerSaveFinishedListenerObserver();
        mDraftAttachmentsAdapter = new DraftAttachmentsAdapter(this, mConversationId, mDraftAttachmentOpener);
        mDraftAttachmentsList.setAdapter(mDraftAttachmentsAdapter);

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
        mDraftAttachmentsList.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
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

        GcmConnector.get().fetchAndRegisterToken(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        GcmConnector.get().removeAllNotifications(this);
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
        int frameHeight = findViewById(R.id.new_message_frame).getHeight();
        int headerHeight = findViewById(R.id.new_message_header).getHeight();

        // Note LinearLayout#getDividerDrawable requires API 16
        int[] attrs = new int[]{ android.R.attr.listDivider };
        Drawable divider;
        {
            final TypedArray a = this.obtainStyledAttributes(attrs);
            divider = a.getDrawable(0);
            a.recycle();
        }
        RuntimeAssertion.require(divider != null);
        int dividerHeight = divider.getIntrinsicHeight();

        // Attachments list
        if (mDraftAttachmentsAdapter.getItemCount() == 0) {
            if (mDraftAttachmentsList.getVisibility() != View.GONE) {
                mDraftAttachmentsList.setVisibility(View.GONE);
            }
        } else {
            if (mDraftAttachmentsList.getVisibility() != View.VISIBLE) {
                mDraftAttachmentsList.setVisibility(View.VISIBLE);
            }
        }

        // Text box
        int attachmentListHeight = 0;
        int extraDividerHeight = 0;
        if (mDraftAttachmentsAdapter.getItemCount() > 0) {
            attachmentListHeight = mDraftAttachmentsList.getHeight()
                + ((LinearLayout.LayoutParams) mDraftAttachmentsList.getLayoutParams()).topMargin
                + ((LinearLayout.LayoutParams) mDraftAttachmentsList.getLayoutParams()).bottomMargin;
            extraDividerHeight = dividerHeight;
            if (attachmentListHeight > frameHeight / 2) {
                // prevent attachment list to take over the whole view (user will have to scroll down past this point)
                attachmentListHeight = frameHeight / 2;
            }
        }

        //Log.d(TAG, "frame: " + frameHeight + ", header: " + headerHeight + ", divider: " + dividerHeight + ", attachments: " + attachmentListHeight + " + " + extraDividerHeight);

        int newMinimumHeight = frameHeight - headerHeight - dividerHeight - attachmentListHeight - extraDividerHeight;
        if (currentMessageTextInputMinimumHeight != newMinimumHeight) {
            mNewMessageText.setMinimumHeight(newMinimumHeight);
            currentMessageTextInputMinimumHeight = newMinimumHeight;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        mNewMessageReceivers.setText(SessionConnector.get().getConversationNameOrPlaceHolder(mConversationId));

        updateDraftTextFromStorage();
        restoreSelection();

        mDraftEventObserver = new DraftEventObserver() {
            @Override
            public void draftStateChanged(long conversationId) {
                Log.w(TAG, "möp");
            }

            @Override
            public void draftTextChanged(long conversationId) {
                mNewMessageText.setText(SessionConnector.get().getDraftText(conversationId));
            }
        };
        SessionConnector.get().addEventObserver(
                DraftEventObserver.class,
                mDraftEventObserver);
    }

    @Override
    protected void onStop() {
        super.onStop();

        SessionConnector.get().removeEventObserver(
                DraftEventObserver.class,
                mDraftEventObserver);
    }

    @Override
    protected void onPause() {
        super.onPause();

        saveDraft(false);
        rememberSelection();
        Log.d(TAG, "Activity paused. Draft saved.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterSyncFinishedListenerObserver();
        mDraftAttachmentOpener.unregisterSaveFinishedListenerObserver();
    }

    private void updateDraftTextFromStorage() {
        //set old draft text, if there is any
        final String draftText = SessionConnector.get().getDraftText(mConversationId);
        if (!draftText.isEmpty()) {
            mNewMessageText.setText(draftText);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: // main back button
                Log.d(TAG, "Main back button clicked");
                finish(); // this will cause onPause, which saves the draft
                return true;
            case R.id.action_add_attachment:
                selectAttachmentIntent();
                return true;
            case R.id.action_send:
                saveDraft(true);
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

    private void saveDraft(boolean sendDraft) {
        String message = mNewMessageText.getText().toString();

        if (!message.isEmpty() || mDraftAttachmentsAdapter.getItemCount() > 0) {
            if (sendDraft) {
                //show waiting dialog
                mProgressSync = new MaterialDialog.Builder(this)
                        .title(R.string.progress_send)
                        .content(R.string.please_wait)
                        .progress(true, 0)
                        .cancelable(false)
                        .show();

                SessionConnector.get().saveDraftForConversation(mConversationId, message, true);
                SessionConnector.get().sendMessages();
            } else {
                SessionConnector.get().saveDraftForConversation(mConversationId, message, false);
            }
        } else {
            SessionConnector.get().clearDraftForConversation(mConversationId);
        }

        setResult(RESULT_OK);
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
            public void progressed(SyncProgress progress) {
            }

            @Override
            public void finished() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mProgressSync != null) {
                            mProgressSync.dismiss();
                        }
                        finish();
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

    private void rememberSelection() {
        mStoredSelection = new Pair<>(mNewMessageText.getSelectionStart(), mNewMessageText.getSelectionEnd());
    }

    private void restoreSelection() {
        if (mStoredSelection != null) {
            int textLength = mNewMessageText.getText().length();
            mNewMessageText.setSelection(
                    Math.min(textLength, mStoredSelection.first),
                    Math.min(textLength, mStoredSelection.second));
        }
    }


    // ATTACHMENTS

    private void selectAttachmentIntent() {
        // Open file for attachment
        final Intent fileIntent = new Intent();
        fileIntent.setType("*/*");
        fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
        fileIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(fileIntent, REQUEST_CODE_ATTACH_FILE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ATTACH_FILE) {
            if (resultCode == Activity.RESULT_OK) {
                RuntimeAssertion.require(data != null);
                final Uri selectedFileUri = data.getData();
                RuntimeAssertion.require(selectedFileUri != null);
                Log.d(TAG, "Attachment source: " + selectedFileUri);

                final String scheme = selectedFileUri.getScheme();
                switch (scheme) {
                    case "file":
                        handleAttachmentFromFileUri(selectedFileUri);
                        break;
                    case "content":
                        handleAttachmentFromContentUri(selectedFileUri);
                        break;
                    default:
                        RuntimeAssertion.fail("Unrecognized scheme: " + scheme);
                }
            } else if (resultCode == Activity.RESULT_CANCELED){
                Toast.makeText(this, R.string.select_file_canceled, Toast.LENGTH_SHORT).show();
            } else {
                RuntimeAssertion.fail("Unknown result code");
            }
        } else {
            RuntimeAssertion.fail("Unknown request code: " + requestCode);
        }
    }

    @NonNull
    private static String mimeTypeOrFallback(String mimeType) {
        if (mimeType != null) return mimeType;
        else return "application/octet-stream";
    }

    private void handleAttachmentFromFileUri(final Uri selectedFileUri) {
        final String guessedMimeType = URLConnection.guessContentTypeFromName(selectedFileUri.toString());
        final String mimeType = mimeTypeOrFallback(guessedMimeType);

        AsyncTask task = SessionConnector.get().addAttachmentToDraft(mConversationId, selectedFileUri.getPath(), mimeType);
        task.waitUntilDone();
    }

    private void handleAttachmentFromContentUri(final Uri selectedFileUri) {
        final String mimeType = mimeTypeOrFallback(getContentResolver().getType(selectedFileUri));

        // copy file in known location for uploading
        String tmpFilename = "tmp.tmp"; // default filename if original can't be retrieved

        // Get filename from stream
        Cursor fileInfoCursor = getContentResolver().query(selectedFileUri, null, null, null, null);
        if (fileInfoCursor != null) {
            fileInfoCursor.moveToFirst();
            int nameIndex = fileInfoCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex != -1) {
                tmpFilename = fileInfoCursor.getString(nameIndex);
            }
            fileInfoCursor.close();
        }

        File appCacheDir = getCacheDir();
        RuntimeAssertion.require(appCacheDir != null);

        final String tmpFilePath = appCacheDir.getPath() + File.separatorChar + tmpFilename;
        Log.d(TAG, "Tmp file path before adding to DB: " + tmpFilePath);

        if (StreamCopy.copyToPath(this, selectedFileUri, tmpFilePath)) {
            AsyncTask task = SessionConnector.get().addAttachmentToDraft(mConversationId, tmpFilePath, mimeType);
            task.waitUntilDone();

            // remove temporary file now that it's been uploaded
            if (!(new File(tmpFilePath)).delete()) {
                Log.e(TAG, "File could not be deleted: " + tmpFilePath);
            }
        } else {
            Log.e(TAG, "Could not copy " + selectedFileUri + " to " + tmpFilePath);
            Toast.makeText(this, R.string.compose_error_loading_file, Toast.LENGTH_SHORT).show();
        }
    }
}
