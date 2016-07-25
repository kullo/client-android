/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.singlemessage;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import net.kullo.android.R;
import net.kullo.android.application.KulloApplication;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.Debug;
import net.kullo.android.littlehelpers.ListHelper;
import net.kullo.android.littlehelpers.StreamCopy;
import net.kullo.android.observers.listenerobservers.MessageAttachmentsSaveListenerObserver;
import net.kullo.javautils.RuntimeAssertion;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MessageAttachmentsOpener implements MessageAttachmentsSaveListenerObserver {
    public static final String TAG = "MessageAttachmentsOp."; // max 23 chars
    private static final int REQUEST_CODE_SAVE_TO = 1001;

    private Activity mBaseActivity;
    private MaterialDialog mSavingAttachmentDialog;

    private enum OpenAction {
        OPEN,
        OPEN_WITH,
        SAVE_TO,
        SHARE,
    }

    private OpenAction mCurrentAction = null;
    private int mPendingFiles = 0;
    private ArrayList<Uri> mReadyFiles = new ArrayList<>();
    private String mCurrentActionMergedMimeType;
    private Uri mCurrentActionSaveToTmpFile;

    public MessageAttachmentsOpener(Activity activity) {
        mBaseActivity = activity;
    }

    ///// PUBLIC API

    public void handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SAVE_TO) {
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "Tmp file: " + mCurrentActionSaveToTmpFile);
                //Log.d(TAG, "Activity result: " + Debug.getIntentDetails(data));
                Uri target = data.getData();
                Log.d(TAG, "Target file: " + target);

                final String scheme = target.getScheme();
                switch (scheme) {
                    case "file":
                        // Move tmp to target
                        File from = new File(mCurrentActionSaveToTmpFile.getPath());
                        File to = new File(target.getPath());
                        if (!from.renameTo(to)) {
                            Log.e(TAG, "Could not rename file. Falling back to copy.");
                            if (!StreamCopy.copyToUri(mBaseActivity, mCurrentActionSaveToTmpFile, target)) {
                                Toast.makeText(mBaseActivity, R.string.singlemessage_file_save_failed, Toast.LENGTH_SHORT).show();
                            }
                        }
                        break;
                    case "content":
                        if (!StreamCopy.copyToUri(mBaseActivity, mCurrentActionSaveToTmpFile, target)) {
                            Toast.makeText(mBaseActivity, R.string.singlemessage_file_save_failed, Toast.LENGTH_SHORT).show();
                        }
                        break;
                    default:
                        RuntimeAssertion.fail("Unrecognized scheme: " + scheme);
                }

            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(mBaseActivity, R.string.singlemessage_file_save_to_canceled, Toast.LENGTH_SHORT).show();
            } else {
                RuntimeAssertion.fail("Unknown result code");
            }
        } else {
            RuntimeAssertion.fail("Unknown request code");
        }
    }

    public void saveAndOpenAttachment(long messageId, long attachmentId) {
        mCurrentAction = OpenAction.OPEN;
        saveAttachments(messageId, ListHelper.asList(attachmentId));
    }

    public void saveAndOpenWithAttachment(long messageId, long attachmentId) {
        mCurrentAction = OpenAction.OPEN_WITH;
        saveAttachments(messageId, ListHelper.asList(attachmentId));
    }

    public void saveAndShareAttachments(long messageId, List<Long> attachmentList) {
        mCurrentAction = OpenAction.SHARE;
        saveAttachments(messageId, attachmentList);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void saveAndDownloadAttachment(long messageId, long attachmentId) {
        mCurrentAction = OpenAction.SAVE_TO;
        saveAttachments(messageId, ListHelper.asList(attachmentId));
    }

    ///// REGISTER OBSERVER

    public void registerSaveFinishedListenerObserver() {
        SessionConnector.get().addListenerObserver(MessageAttachmentsSaveListenerObserver.class, this);
    }

    public void unregisterSaveFinishedListenerObserver() {
        SessionConnector.get().removeListenerObserver(MessageAttachmentsSaveListenerObserver.class, this);
    }

    ///// PRIVATE METHODS

    private void saveAttachments(final long messageId, final List<Long> attachmentList) {
        mPendingFiles = 0;
        mReadyFiles.clear();
        mCurrentActionMergedMimeType = "";

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
        if (mCurrentAction.equals(OpenAction.OPEN) || mCurrentAction.equals(OpenAction.OPEN_WITH)) {
            if (!((KulloApplication) mBaseActivity.getApplication()).canOpenFileType(file, mimeType)) {
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
        mCurrentActionMergedMimeType = mergeMimeType(mCurrentActionMergedMimeType, mimeType);
    }

    private void processFiles() {
        Intent deliverIntent = new Intent();

        Log.d(TAG, "Processing files ...");

        switch (mCurrentAction) {
            case OPEN:
                deliverIntent.setAction(Intent.ACTION_VIEW);
                break;
            case OPEN_WITH:
                deliverIntent.setAction(Intent.ACTION_VIEW);
                break;
            case SAVE_TO:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    deliverIntent.setAction(Intent.ACTION_CREATE_DOCUMENT);
                    deliverIntent.addCategory(Intent.CATEGORY_OPENABLE);
                }
                break;
            case SHARE:
                if (mReadyFiles.size() == 1) {
                    // Some share receivers only register for single file action SEND.
                    // Thus don't send a general SEND_MULTIPLE when only one file is shared.
                    deliverIntent.setAction(Intent.ACTION_SEND);
                } else {
                    deliverIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
                }
                break;
            default:
                RuntimeAssertion.fail("Unhandled enum value");
        }

        if (mCurrentAction.equals(OpenAction.SHARE)) {
            if (mReadyFiles.size() == 1) {
                deliverIntent.putExtra(Intent.EXTRA_STREAM, mReadyFiles.get(0));
            } else {
                deliverIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, mReadyFiles);
            }
            deliverIntent.setType(mCurrentActionMergedMimeType);
        } else if (mCurrentAction.equals(OpenAction.SAVE_TO)) {
            mCurrentActionSaveToTmpFile = mReadyFiles.get(0);
            deliverIntent.setType(mCurrentActionMergedMimeType);
            String filename = mCurrentActionSaveToTmpFile.getLastPathSegment();
            deliverIntent.putExtra(Intent.EXTRA_TITLE, filename);
        } else {
            mCurrentActionSaveToTmpFile = mReadyFiles.get(0);
            deliverIntent.setDataAndType(mCurrentActionSaveToTmpFile, mCurrentActionMergedMimeType);
            String filename = mCurrentActionSaveToTmpFile.getLastPathSegment();
            deliverIntent.putExtra(Intent.EXTRA_TITLE, filename);
        }

        Log.d(TAG, "Intent: " + Debug.getIntentDetails(deliverIntent));

        // Launch action
        switch (mCurrentAction) {
            case OPEN:
                mBaseActivity.startActivity(deliverIntent);
                break;
            case OPEN_WITH:
                mBaseActivity.startActivity(Intent.createChooser(deliverIntent, chooserTitle(mCurrentAction)));
                break;
            case SAVE_TO:
                mBaseActivity.startActivityForResult(deliverIntent, REQUEST_CODE_SAVE_TO);
                break;
            case SHARE:
                mBaseActivity.startActivity(Intent.createChooser(deliverIntent, chooserTitle(mCurrentAction)));
                break;
            default:
                RuntimeAssertion.fail("Unhandled enum value");
        }
    }

    private String chooserTitle(OpenAction currentAction) {
        switch (currentAction) {
            case OPEN_WITH:
                return mBaseActivity.getResources().getString(R.string.action_open_with);
            case SHARE:
                return mBaseActivity.getResources().getString(R.string.action_share);
            default:
                RuntimeAssertion.fail("Given enum value has no title");
                return "";
        }
    }

}
