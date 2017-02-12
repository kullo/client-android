/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.singlemessage;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import net.kullo.android.R;
import net.kullo.android.application.CacheType;
import net.kullo.android.application.KulloApplication;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.Debug;
import net.kullo.android.littlehelpers.MimeTypeMerger;
import net.kullo.android.littlehelpers.StreamCopy;
import net.kullo.android.observers.listenerobservers.MessageAttachmentsSaveListenerObserver;
import net.kullo.javautils.RuntimeAssertion;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.dialogsforandroid.DialogAction;
import io.github.dialogsforandroid.MaterialDialog;

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

    private class ReadyFile {
        @NonNull public File tmpfile;
        @NonNull public String filename;
        @NonNull public Uri uri;

        public ReadyFile(@NonNull File tmpfile_, @NonNull String filename_, @NonNull Uri uri_) {
            tmpfile = tmpfile_;
            filename = filename_;
            uri = uri_;
        }
    }

    private OpenAction mCurrentAction = null;
    private int mPendingFiles = 0;
    private ArrayList<ReadyFile> mReadyFiles = new ArrayList<>();
    private String mCurrentActionMergedMimeType;
    private File mCurrentActionSaveToTmpFile;

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
                        File from = mCurrentActionSaveToTmpFile;
                        File to = new File(target.getPath());
                        if (!from.renameTo(to)) {
                            Log.e(TAG, "Could not rename file. Falling back to copy.");
                            if (!StreamCopy.copyToUri(mBaseActivity, Uri.fromFile(mCurrentActionSaveToTmpFile), target)) {
                                Toast.makeText(mBaseActivity, R.string.singlemessage_file_save_failed, Toast.LENGTH_SHORT).show();
                            }
                        }
                        break;
                    case "content":
                        if (!StreamCopy.copyToUri(mBaseActivity, Uri.fromFile(mCurrentActionSaveToTmpFile), target)) {
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
        saveAttachments(messageId, Collections.singletonList(attachmentId));
    }

    public void saveAndOpenWithAttachment(long messageId, long attachmentId) {
        mCurrentAction = OpenAction.OPEN_WITH;
        saveAttachments(messageId, Collections.singletonList(attachmentId));
    }

    public void saveAndShareAttachments(long messageId, List<Long> attachmentList) {
        mCurrentAction = OpenAction.SHARE;
        saveAttachments(messageId, attachmentList);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void saveAndDownloadAttachment(long messageId, long attachmentId) {
        mCurrentAction = OpenAction.SAVE_TO;
        saveAttachments(messageId, Collections.singletonList(attachmentId));
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

        List<String> mimeTypes = new ArrayList<>();
        for (long attachmentId : attachmentList) {
            mimeTypes.add(SessionConnector.get().getMessageAttachmentMimeType(messageId, attachmentId));
        }
        mCurrentActionMergedMimeType = MimeTypeMerger.merge(mimeTypes);

        ArrayList<File> outputFiles = new ArrayList<>();
        for (long attachmentId : attachmentList) {
            File tmpfile = getTmpFilepathForAttachment(messageId, attachmentId);
            if (tmpfile == null) {
                // cannot open a file
                return;
            }

            outputFiles.add(tmpfile);
        }

        mPendingFiles = attachmentList.size();
        createSavingDialog();
        for (int i = 0; i < attachmentList.size(); i++) {
            startSavingFile(messageId, attachmentList.get(i), outputFiles.get(i));
        }
    }

    @Nullable
    private File getTmpFilepathForAttachment(final long messageId, final long attachmentId) {
        String filename = SessionConnector.get().getMessageAttachmentFilename(messageId, attachmentId);

        final String subfolder = "attachment_" + messageId + "-" + attachmentId;
        final File fileOpenCacheDir = ((KulloApplication) mBaseActivity.getApplication()).cacheDir(CacheType.OpenFile, subfolder);
        final File tmpfile = new File(fileOpenCacheDir, filename);

        if (mCurrentAction.equals(OpenAction.OPEN) || mCurrentAction.equals(OpenAction.OPEN_WITH)) {
            {
                final String mimeType = SessionConnector.get().getMessageAttachmentMimeType(messageId, attachmentId);
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
                    return null;
                }
            }
        }
        return tmpfile;
    }

    private void createSavingDialog() {
        mSavingAttachmentDialog = new MaterialDialog.Builder(mBaseActivity)
                .title(R.string.exporting_to_downloads_folder)
                .content(R.string.please_wait)
                .progress(true, 0)
                .cancelable(false)
                .show();
    }

    @MainThread
    private void startSavingFile(long messageId, long attachmentId, final File destination) {
        Log.d(TAG, "Saving file to: " + destination.getAbsolutePath());
        SessionConnector.get().saveMessageAttachment(messageId, attachmentId, destination.getAbsolutePath());
    }

    @Override
    public void finished(final long messageId, final long attachmentId, final String path) {
        mBaseActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                prepareTmpfileForProcess(path);
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

    @MainThread
    private void prepareTmpfileForProcess(final String path) {
        mPendingFiles--;
        File tmpfile = new File(path);
        String filename = tmpfile.getName();
        Uri uri = FileProvider.getUriForFile(mBaseActivity.getApplication(), KulloApplication.ID, tmpfile);
        Log.d(TAG, "Serving URI: " + uri);
        mReadyFiles.add(new ReadyFile(tmpfile, filename, uri));
    }

    private void processFiles() {
        Intent deliverIntent = new Intent();
        deliverIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

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
                ReadyFile rf = mReadyFiles.get(0);
                deliverIntent.putExtra(Intent.EXTRA_STREAM, rf.uri);
            } else {
                ArrayList<Uri> uris = new ArrayList<>();
                for (ReadyFile rf : mReadyFiles) {
                    uris.add(rf.uri);
                }
                deliverIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            }
            deliverIntent.setType(mCurrentActionMergedMimeType);
        } else if (mCurrentAction.equals(OpenAction.SAVE_TO)) {
            ReadyFile rf = mReadyFiles.get(0);
            mCurrentActionSaveToTmpFile = rf.tmpfile;
            deliverIntent.putExtra(Intent.EXTRA_TITLE, rf.filename);
            deliverIntent.setType(mCurrentActionMergedMimeType);
        } else {
            // open / open with
            ReadyFile rf = mReadyFiles.get(0);
            deliverIntent.setDataAndType(rf.uri, mCurrentActionMergedMimeType);
            deliverIntent.putExtra(Intent.EXTRA_TITLE, rf.filename);
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
