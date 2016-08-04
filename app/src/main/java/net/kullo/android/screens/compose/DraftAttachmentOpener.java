/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.compose;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import net.kullo.android.R;
import net.kullo.android.application.KulloApplication;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.observers.listenerobservers.DraftAttachmentsSaveListenerObserver;

import java.io.File;

public class DraftAttachmentOpener implements DraftAttachmentsSaveListenerObserver {
    public static final String TAG = "DraftAttachmentOpener";

    private Activity mBaseActivity;
    private MaterialDialog mSavingAttachmentDialog;

    public DraftAttachmentOpener(Activity activity) {
        mBaseActivity = activity;
    }

    public void saveAndOpenAttachment(long conversationId, long attachmentId) {
        final String filename = SessionConnector.get().getDraftAttachmentFilename(conversationId, attachmentId);
        final File fileOpenCacheDir = ((KulloApplication) mBaseActivity.getApplication()).fileOpenCacheDir();
        final File tmpfile = new File(fileOpenCacheDir, filename);

        {
            // Check this in advance to avoid copying file from DB to cache when it cannot be opened
            final String mimeType = SessionConnector.get().getDraftAttachmentMimeType(conversationId, attachmentId);
            if (!((KulloApplication) mBaseActivity.getApplication()).canOpenFileType(tmpfile, mimeType)) {
                Log.d(TAG, "Cannot open file: '" + tmpfile + "' mimeType: '" + mimeType + "'");
                new MaterialDialog.Builder(mBaseActivity)
                        .title(R.string.attachments_unknown_mimetype_title)
                        .content(R.string.attachments_unknown_mimetype_text)
                        .positiveText(R.string.ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
                return;
            }
        }

        mSavingAttachmentDialog = new MaterialDialog.Builder(mBaseActivity)
                .title(R.string.exporting_to_downloads_folder)
                .content(R.string.please_wait)
                .progress(true, 0)
                .cancelable(false)
                .show();

        Log.d(TAG, "Saving file to: " + tmpfile.getAbsolutePath());
        SessionConnector.get().saveDraftAttachmentContent(conversationId, attachmentId, tmpfile.getAbsolutePath());
    }

    public void registerSaveFinishedListenerObserver() {
        SessionConnector.get().addListenerObserver(
                DraftAttachmentsSaveListenerObserver.class,
                this);
    }

    public void unregisterSaveFinishedListenerObserver() {
        SessionConnector.get().removeListenerObserver(
                DraftAttachmentsSaveListenerObserver.class,
                this);
    }

    @Override
    public void finished(final long conversationId, final long attachmentId, final String path) {
        mBaseActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mSavingAttachmentDialog != null) {
                    mSavingAttachmentDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        public void onDismiss(DialogInterface dialog) {
                            openFile(conversationId, attachmentId, path);
                        }
                    });

                    mSavingAttachmentDialog.dismiss();
                }
            }
        });
    }

    @Override
    public void error(long conversationId, long attachmentId, String path, final String error) {
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

    private void openFile(long conversationId, long attachmentId, String path) {
        String mimeType = SessionConnector.get().getDraftAttachmentMimeType(conversationId, attachmentId);

        File tmpfile = new File(path);
        Uri uri = FileProvider.getUriForFile(mBaseActivity, KulloApplication.ID, tmpfile);
        Log.d(TAG, "Providing URI: " + uri);

        Intent openFileIntent = new Intent();
        openFileIntent.setAction(Intent.ACTION_VIEW);
        openFileIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        openFileIntent.setDataAndType(uri, mimeType);

        mBaseActivity.startActivity(openFileIntent);
    }
}
