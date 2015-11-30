/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import net.kullo.android.R;
import net.kullo.android.kulloapi.KulloConnector;
import net.kullo.android.kulloapi.KulloUtils;
import net.kullo.android.littlehelpers.Debug;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.android.observers.eventobservers.DraftAttachmentAddedEventObserver;
import net.kullo.android.observers.eventobservers.DraftAttachmentRemovedEventObserver;
import net.kullo.android.observers.eventobservers.DraftEventObserver;
import net.kullo.android.observers.listenerobservers.SyncerRunListenerObserver;
import net.kullo.android.screens.compose.DraftAttachmentOpener;
import net.kullo.android.screens.compose.DraftAttachmentsAdapter;
import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.AsyncTask;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ComposeActivity extends AppCompatActivity {
    private static final String TAG = "ComposeActivity";

    private long mConversationId;
    private EditText mNewMessageText;
    private TextView mNewMessageReceivers;
    private RecyclerView mDraftAttachmentsList;
    private MaterialDialog mProgressSync;
    private DraftEventObserver mDraftEventObserver;
    private SyncerRunListenerObserver mSyncerRunListenerObserver;
    private DraftAttachmentsAdapter mDraftAttachmentsAdapter;
    private DraftAttachmentOpener mDraftAttachmentOpener;

    public static final int FILE_REQUEST_CODE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AsyncTask task = KulloConnector.get().createActivityWithSession(this);

        setContentView(R.layout.activity_compose);

        Intent intent = getIntent();
        if (intent.hasExtra(KulloConstants.CONVERSATION_ID)) {
            mConversationId = intent.getLongExtra(KulloConstants.CONVERSATION_ID, -1);
            RuntimeAssertion.require(mConversationId != -1);
        } else if (intent.hasExtra(KulloConstants.CONVERSATION_RECIPIENT)) {
            String recipient = intent.getStringExtra(KulloConstants.CONVERSATION_RECIPIENT);
            mConversationId = KulloConnector.get().startConversationWithSingleRecipient(recipient);
        } else if (intent.getData().getScheme().equals("kullo")) {
            Log.d(TAG, Debug.getIntentDetails(intent));

            String addressString = intent.getData().getSchemeSpecificPart();
            if (KulloUtils.isValidKulloAddress(addressString)) {
                mConversationId = KulloConnector.get().startConversationWithSingleRecipient(addressString);
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
        LinearLayoutManager layoutManager = new org.solovyev.android.views.llm.LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mDraftAttachmentsList.setLayoutManager(layoutManager);

        mDraftAttachmentOpener = new DraftAttachmentOpener(this);
        mDraftAttachmentOpener.registerSaveFinishedListenerObserver();
        mDraftAttachmentsAdapter = new DraftAttachmentsAdapter(this, mConversationId, mDraftAttachmentOpener);
        mDraftAttachmentsList.setAdapter(mDraftAttachmentsAdapter);

        KulloConnector.get().addEventObserver(DraftAttachmentAddedEventObserver.class, new DraftAttachmentAddedEventObserver() {
            public void draftAttachmentAdded(long conversationId, long attachmentId) {
                mDraftAttachmentsAdapter.append(attachmentId);
                resizeMessageTextEdit();
            }
        });

        KulloConnector.get().addEventObserver(DraftAttachmentRemovedEventObserver.class, new DraftAttachmentRemovedEventObserver() {
            public void draftAttachmentRemoved(long conversationId, long attachmentId) {
                mDraftAttachmentsAdapter.remove(attachmentId);
            }
        });

        // resize edit window when attachments added or removed
        mDraftAttachmentsList.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                resizeMessageTextEdit();
            }
        });

        registerSyncFinishedListenerObserver();

        if (task != null) task.waitUntilDone();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // onWindowFocusChanged() is called late enough for this
        // http://stackoverflow.com/a/6560726/2013738
        resizeMessageTextEdit();
    }

    protected void resizeMessageTextEdit() {
        // Make message input box fill entire available space such that the user can focus it by
        // touching the whole space. This must be done after we set recipients and know the
        // final header size.
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

        // attachments
        int attachmentListHeight, extraDividerHeight;
        if (mDraftAttachmentsAdapter.getItemCount() == 0) {
            mDraftAttachmentsList.setVisibility(View.GONE);
            attachmentListHeight = 0;
            extraDividerHeight = 0;
        } else {
            mDraftAttachmentsList.setVisibility(View.VISIBLE);
            attachmentListHeight = mDraftAttachmentsList.getHeight()
                + ((LinearLayout.LayoutParams)mDraftAttachmentsList.getLayoutParams()).topMargin
                + ((LinearLayout.LayoutParams)mDraftAttachmentsList.getLayoutParams()).bottomMargin;
            extraDividerHeight = dividerHeight;
            if (attachmentListHeight > frameHeight / 2) {
                // prevent attachment list to take over the whole view (user will have to scroll down past this point)
                attachmentListHeight = frameHeight / 2;
            }
        }

        Log.d(TAG, "frame: " + frameHeight + ", header: " + headerHeight + ", divider: " + dividerHeight + ", attachments: " + attachmentListHeight + " + " + extraDividerHeight);
        mNewMessageText.setMinimumHeight(frameHeight - headerHeight - dividerHeight - attachmentListHeight - extraDividerHeight);
    }

    @Override
    protected void onStart() {
        super.onStart();

        mNewMessageReceivers.setText(KulloConnector.get().getConversationNameOrPlaceHolder(mConversationId));

        updateDraftTextFromStorage();

        mDraftEventObserver = new DraftEventObserver() {
            @Override
            public void draftStateChanged(long conversationId) {
                Log.w(TAG, "möp");
            }

            @Override
            public void draftTextChanged(long conversationId) {
                mNewMessageText.setText(KulloConnector.get().getDraftText(conversationId));
            }
        };
        KulloConnector.get().addEventObserver(
                DraftEventObserver.class,
                mDraftEventObserver);
    }

    @Override
    protected void onStop() {
        super.onStop();

        KulloConnector.get().removeEventObserver(
                DraftEventObserver.class,
                mDraftEventObserver);
    }

    @Override
    protected void onPause() {
        super.onPause();

        saveDraft(false);
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
        String draft = KulloConnector.get().getDraftText(mConversationId);
        if (draft != null && !draft.isEmpty()) {
            mNewMessageText.setText(KulloConnector.get().getDraftText(mConversationId));
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

        }
        return super.onOptionsItemSelected(item);
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

                KulloConnector.get().saveDraftForConversation(mConversationId, message, true);
                KulloConnector.get().sendMessages();
            } else {
                KulloConnector.get().saveDraftForConversation(mConversationId, message, false);
            }
        } else {
            KulloConnector.get().clearDraftForConversation(mConversationId);
        }

        setResult(RESULT_OK);
    }

    private void registerSyncFinishedListenerObserver() {
        mSyncerRunListenerObserver = new SyncerRunListenerObserver() {
            @Override
            public void draftAttachmentsTooBig(long convId) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //TODO error message draftAttachmentsTooBig
                        if (mProgressSync != null) {
                            mProgressSync.dismiss();
                        }
                    }
                });
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
            public void error(final String errorText) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mProgressSync != null) {
                            mProgressSync.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                public void onDismiss(DialogInterface dialog) {
                                    new MaterialDialog.Builder(ComposeActivity.this)
                                            .title(R.string.error_title)
                                            .content(errorText)
                                            .neutralText(R.string.ok)
                                            .cancelable(false)
                                            .show();
                                }
                            });

                            mProgressSync.dismiss();
                        }
                    }
                });
            }
        };
        KulloConnector.get().addListenerObserver(SyncerRunListenerObserver.class,
                mSyncerRunListenerObserver);
    }

    private void unregisterSyncFinishedListenerObserver() {
        KulloConnector.get().removeListenerObserver(SyncerRunListenerObserver.class,
                mSyncerRunListenerObserver);
    }


    // ATTACHMENTS

    private void selectAttachmentIntent() {
        // Open file for attachment
        final Intent fileIntent = new Intent();
        fileIntent.setType("*/*");
        fileIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(fileIntent, FILE_REQUEST_CODE);
    }

     @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == FILE_REQUEST_CODE) {
                final Uri selectedFileUri = data == null ? null : data.getData();
                if (selectedFileUri == null) {
                    Toast.makeText(this.getApplicationContext(),
                    R.string.select_file_failed, Toast.LENGTH_SHORT)
                    .show();
                    return;
                }

                final String mimeType = getContentResolver().getType(selectedFileUri);

                final Context context = this;
                new Thread(new Runnable() {
                    public void run() {
                        // copy file in known location for uploading
                        String fileName = "tmp.tmp"; // default filename if original can't be retrieved

                        // Get filename from stream
                        Cursor fileInfoCursor = getContentResolver().query(selectedFileUri, null, null, null, null);
                        if (fileInfoCursor != null) {
                            fileInfoCursor.moveToFirst();
                            int nameIndex = fileInfoCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                            if (nameIndex != -1) {
                                fileName = fileInfoCursor.getString(nameIndex);
                            }
                            fileInfoCursor.close();
                        }

                        File tmpDir = getCacheDir();
                        if (tmpDir == null) {
                            tmpDir = getExternalCacheDir();
                            if (tmpDir == null) {
                                tmpDir = android.os.Environment.getExternalStorageDirectory();
                                if (tmpDir == null) {
                                    Log.e(TAG, "Could not open any tmp directory for writing");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(context.getApplicationContext(),
                                                R.string.no_tmp_dir_found, Toast.LENGTH_SHORT)
                                                .show();
                                        }
                                    });
                                    return;
                                }
                            }
                        }

                        String destPath = tmpDir.getPath() + File.separatorChar + fileName;

                        InputStream inputFile = null;
                        BufferedOutputStream outputFile = null;

                        try {
                            inputFile = getContentResolver().openInputStream(selectedFileUri);
                            RuntimeAssertion.require(inputFile != null);
                            outputFile = new BufferedOutputStream(new FileOutputStream(destPath, false));
                            byte[] buffer = new byte[1024];

                            int bytesRead;
                            while ((bytesRead = inputFile.read(buffer)) != -1) {
                                outputFile.write(buffer, 0, bytesRead);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                if (inputFile != null) inputFile.close();
                                if (outputFile != null) outputFile.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        AsyncTask task = KulloConnector.get().addAttachmentToDraft(mConversationId, destPath, mimeType);
                        if (task != null) task.waitUntilDone();

                        // remove temporary file now that it's been uploaded
                        if (!(new File(destPath)).delete()) {
                            Log.e(TAG, "File could not be deleted: '" + destPath + "'");
                        }
                    }
                }).start();
            }

        } else if (resultCode == Activity.RESULT_CANCELED){
            Toast.makeText(this.getApplicationContext(),
                    R.string.select_file_canceled, Toast.LENGTH_SHORT)
                    .show();
        } else {
            Toast.makeText(this.getApplicationContext(),
                    R.string.select_file_failed, Toast.LENGTH_SHORT)
                    .show();
        }
    }
}
