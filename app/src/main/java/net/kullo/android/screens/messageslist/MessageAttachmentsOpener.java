/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.messageslist;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;

import net.kullo.android.R;
import net.kullo.android.application.KulloApplication;
import net.kullo.android.kulloapi.KulloConnector;
import net.kullo.android.observers.listenerobservers.MessageAttachmentsSaveListenerObserver;

import java.io.File;

public class MessageAttachmentsOpener implements MessageAttachmentsSaveListenerObserver {
    public static final String TAG = "MessageAttachmentsOp."; // max 23 chars

    private Activity mBaseActivity;
    private MaterialDialog mSavingAttachmentDialog;

    public MessageAttachmentsOpener(Activity activity) {
        mBaseActivity = activity;
    }

    public void saveAndOpenAttachment(long messageId, long attachmentId) {
        String filename = KulloConnector.get().getMessageAttachmentFilename(messageId, attachmentId);
        String mimeType = KulloConnector.get().getMessageAttachmentMimeType(messageId, attachmentId);
        File tmpDirectory = mBaseActivity.getExternalFilesDir("tmp");
        if (tmpDirectory == null) {
            Log.d(TAG, "Cannot open tmp directory for storing attachments");
            new MaterialDialog.Builder(mBaseActivity)
                    .title(R.string.attachments_cannot_open_externaldir_title)
                    .content(R.string.attachments_cannot_open_externaldir_text)
                    .positiveText(R.string.ok)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            dialog.dismiss();
                        }
                    })
                    .show();
            return;
        }

        File file = new File(tmpDirectory.getPath(), filename);

        if (!((KulloApplication) mBaseActivity.getApplication()).canOpenFileType(file, mimeType)) {
            Log.d(TAG, "Cannot open file: '" + file + "' mimeType: '" + mimeType + "'");
            new MaterialDialog.Builder(mBaseActivity)
                    .title(R.string.attachments_unknown_mimetype_title)
                    .content(R.string.attachments_unknown_mimetype_text)
                    .positiveText(R.string.ok)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            dialog.dismiss();
                        }
                    })
                    .show();
            return;
        }

        mSavingAttachmentDialog = new MaterialDialog.Builder(mBaseActivity)
                .title(R.string.exporting_to_downloads_folder)
                .content(R.string.please_wait)
                .progress(true, 0)
                .cancelable(false)
                .show();

        Log.d(TAG, "Saving file to: " + file.getAbsolutePath());
        KulloConnector.get().saveMessageAttachment(messageId, attachmentId, file.getAbsolutePath());
    }

    public void registerSaveFinishedListenerObserver() {
        KulloConnector.get().addListenerObserver(MessageAttachmentsSaveListenerObserver.class, this);
    }

    public void unregisterSaveFinishedListenerObserver() {
        KulloConnector.get().removeListenerObserver(MessageAttachmentsSaveListenerObserver.class, this);
    }

    @Override
    public void finished(final long messageId, final long attachmentId, final String path) {
        mBaseActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mSavingAttachmentDialog != null) {
                    mSavingAttachmentDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        public void onDismiss(DialogInterface dialog) {
                            openFile(messageId, attachmentId, path);
                        }
                    });

                    mSavingAttachmentDialog.dismiss();
                }
            }
        });
    }

    @Override
    public void error(long messageId, long attachmentId, String path, final String error) {
        mBaseActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mSavingAttachmentDialog != null) {
                    mSavingAttachmentDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        public void onDismiss(DialogInterface dialog) {
                            new MaterialDialog.Builder(mBaseActivity)
                                    .title(R.string.error_title)
                                    .content(error)
                                    .neutralText(R.string.ok)
                                    .cancelable(false)
                                    .show();
                        }});

                    mSavingAttachmentDialog.dismiss();
                }
            }
        });
    }

    private void openFile(long messageId, long attachmentId, String path) {
        String mimeType = KulloConnector.get().getMessageAttachmentMimeType(messageId, attachmentId);

        Intent openFileIntent = new Intent();
        openFileIntent.setAction(android.content.Intent.ACTION_VIEW);
        openFileIntent.setDataAndType(Uri.fromFile(new File(path)), mimeType);

        mBaseActivity.startActivity(openFileIntent);
    }
}
