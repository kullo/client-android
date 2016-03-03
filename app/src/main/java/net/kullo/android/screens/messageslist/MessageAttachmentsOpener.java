/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.messageslist;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import net.kullo.android.R;
import net.kullo.android.application.KulloApplication;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.observers.listenerobservers.MessageAttachmentsSaveListenerObserver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MessageAttachmentsOpener implements MessageAttachmentsSaveListenerObserver {
    public static final String TAG = "MessageAttachmentsOp."; // max 23 chars

    private Activity mBaseActivity;
    private MaterialDialog mSavingAttachmentDialog;

    private String mCurrentAction = android.content.Intent.ACTION_VIEW;
    private boolean mForceChooser = false;
    private int mPendingFiles = 0;
    private ArrayList<Uri> mReadyFiles = new ArrayList<>();
    private String mMimeType;
    private String mChooserTitle;

    public MessageAttachmentsOpener(Activity activity) {
        mBaseActivity = activity;
    }

    ///// PUBLIC API

    public void saveAndOpenAttachment(long messageId, long attachmentId) {
        mCurrentAction = android.content.Intent.ACTION_VIEW;
        mForceChooser = false;

        ArrayList<Long> attachmentList = new ArrayList<>();
        attachmentList.add(attachmentId);
        saveAttachmentGroup(messageId, attachmentList);
    }

    public void saveAndOpenWithAttachment(long messageId, long attachmentId) {
        mCurrentAction = android.content.Intent.ACTION_VIEW;
        mForceChooser = true;
        mChooserTitle = mBaseActivity.getResources().getString(R.string.action_open_with);

        ArrayList<Long> attachmentList = new ArrayList<>();
        attachmentList.add(attachmentId);
        saveAttachmentGroup(messageId, attachmentList);
    }

    public void saveAndShareAttachments(long messageId, List<Long> attachmentList) {
        mCurrentAction = android.content.Intent.ACTION_SEND_MULTIPLE;
        mForceChooser = true;
        mChooserTitle = mBaseActivity.getResources().getString(R.string.action_share);

        saveAttachmentGroup(messageId, attachmentList);
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void saveAndDownloadAttachment(long messageId, long attachmentId) {
        mCurrentAction = Intent.ACTION_CREATE_DOCUMENT;
        mForceChooser = false;
        ArrayList<Long> attachmentList = new ArrayList<>();
        attachmentList.add(attachmentId);
        saveAttachmentGroup(messageId, attachmentList);
    }

    ///// REGISTER OBSERVER

    public void registerSaveFinishedListenerObserver() {
        SessionConnector.get().addListenerObserver(MessageAttachmentsSaveListenerObserver.class, this);
    }

    public void unregisterSaveFinishedListenerObserver() {
        SessionConnector.get().removeListenerObserver(MessageAttachmentsSaveListenerObserver.class, this);
    }

    ///// PRIVATE METHODS

    private void saveAttachmentGroup(final long messageId, final List<Long> attachmentList) {
        mPendingFiles = 0;
        mReadyFiles.clear();
        mMimeType = "";

        ArrayList<File> outputFiles = new ArrayList<>();
        for (long attachmentId : attachmentList) {
            File fileHandle = getFileHandleForAttachment(messageId, attachmentId);
            if (fileHandle != null) outputFiles.add(fileHandle);
        }

        if (outputFiles.size() == attachmentList.size()) {
            // all passed
            mPendingFiles = attachmentList.size();
            createSavingDialog();
            for (int i = 0; i < attachmentList.size(); i++) {
                startSavingFile(messageId, attachmentList.get(i), outputFiles.get(i));
            }
        }
    }

    @Nullable
    private File getFileHandleForAttachment(final long messageId, final long attachmentId) {
        String filename = SessionConnector.get().getMessageAttachmentFilename(messageId, attachmentId);
        String mimeType = SessionConnector.get().getMessageAttachmentMimeType(messageId, attachmentId);

        File tmpDirectory = mBaseActivity.getExternalFilesDir("tmp");
        if (tmpDirectory == null) {
            Log.d(TAG, "Cannot open tmp directory for storing attachments");
            new MaterialDialog.Builder(mBaseActivity)
                    .title(R.string.attachments_cannot_open_externaldir_title)
                    .content(R.string.attachments_cannot_open_externaldir_text)
                    .positiveText(R.string.ok)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
            return null;
        }

        File file = new File(tmpDirectory.getPath(), filename);
        if (mCurrentAction.equals(android.content.Intent.ACTION_VIEW) &&
            !((KulloApplication) mBaseActivity.getApplication()).canOpenFileType(file, mimeType)) {
            Log.d(TAG, "Cannot open file: '" + file + "' mimeType: '" + mimeType + "'");
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
            return null;
        }
        return file;
    }

    private void createSavingDialog() {
        mSavingAttachmentDialog = new MaterialDialog.Builder(mBaseActivity)
                .title(R.string.exporting_to_downloads_folder)
                .content(R.string.please_wait)
                .progress(true, 0)
                .cancelable(false)
                .show();
    }

    private void startSavingFile(long messageId, long attachmentId, final File file) {
        Log.d(TAG, "Saving file to: " + file.getAbsolutePath());
        SessionConnector.get().saveMessageAttachment(messageId, attachmentId, file.getAbsolutePath());
    }

    @Override
    public void finished(final long messageId, final long attachmentId, final String path) {
        mBaseActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                prepareEntryForProcess(messageId, attachmentId, path);
                if (mPendingFiles == 0) {
                    if (mSavingAttachmentDialog != null) {
                        mSavingAttachmentDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            public void onDismiss(DialogInterface dialog) {
                                processFiles();
                            }
                        });

                        mSavingAttachmentDialog.dismiss();
                    }
                }
            }
        });
    }

    @Override
    public void error(long messageId, long attachmentId, String path, final String error) {
        mPendingFiles = -1; // prevent further files to be processed
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

    private String mergeMimeType(String currentMimeType, String newMimeType) {
        if (currentMimeType == null || currentMimeType.length() == 0) {
            return newMimeType;
        }

        if (currentMimeType.equals(newMimeType)) {
            // already correct
            return currentMimeType;
        }

        String firstNew = newMimeType.substring(0, newMimeType.indexOf('/'));
        String firstCurrent = currentMimeType.substring(0, currentMimeType.indexOf('/'));
        if (firstCurrent.equals(firstNew)) {
            return firstNew + "/*";
        } else {
            return "*/*";
        }

    }

    private void prepareEntryForProcess(final long messageId, final long attachmentId, final String path) {
        mPendingFiles--;
        mReadyFiles.add(Uri.fromFile(new File(path)));
        String mimeType = SessionConnector.get().getMessageAttachmentMimeType(messageId, attachmentId);
        mMimeType = mergeMimeType(mMimeType, mimeType);
    }

    private boolean currentActionIsForSingleFile() {
        switch(mCurrentAction) {
            case Intent.ACTION_VIEW: return true;
            case Intent.ACTION_CREATE_DOCUMENT: return true;
            default:
                return false;
        }
    }

    private void processFiles() {
        Intent deliverIntent = new Intent();
        deliverIntent.setAction(mCurrentAction);

        if (currentActionIsForSingleFile()) {
            Uri filePath = mReadyFiles.get(0);
            deliverIntent.setDataAndType(filePath, mMimeType);
            deliverIntent.putExtra(Intent.EXTRA_TITLE, filePath.getLastPathSegment());
        } else {
            deliverIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, mReadyFiles);
            deliverIntent.setType(mMimeType);
        }

        if (!mForceChooser) {
            mBaseActivity.startActivity(deliverIntent);
        } else {
            mBaseActivity.startActivity(Intent.createChooser(deliverIntent, mChooserTitle));
        }
    }
}
