/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import net.kullo.android.R;
import net.kullo.android.application.CacheType;
import net.kullo.android.application.KulloApplication;
import net.kullo.android.kulloapi.CreateSessionResult;
import net.kullo.android.kulloapi.CreateSessionState;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.AvatarUtils;
import net.kullo.android.littlehelpers.Formatting;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.littlehelpers.StreamCopy;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.android.littlehelpers.UriHelpers;
import net.kullo.android.notifications.GcmConnector;
import net.kullo.android.screens.sharereceiver.FilesAdapter;
import net.kullo.android.screens.sharereceiver.ImagesPreviewAdapter;
import net.kullo.android.screens.sharereceiver.Share;
import net.kullo.android.screens.sharereceiver.ShareTargetsAdapter;
import net.kullo.android.ui.DividerDecoration;
import net.kullo.android.ui.ImageView16by9;
import net.kullo.android.ui.RecyclerItemClickListener;
import net.kullo.javautils.RuntimeAssertion;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class ShareReceiverActivity extends AppCompatActivity {
    private static String TAG = "ShareReceiverActivity";

    // Preview
    private FilesAdapter mFilesAdapter;
    private ImagesPreviewAdapter mImagesPreviewAdapter;
    private MaterialProgressBar mReadFilesProgressIndicator;
    private RecyclerView mPreviewContainerFiles;
    private RecyclerView mPreviewContainerMultipleImages;
    private View mPreviewContainerSingleImage;
    private CardView mPreviewContainerText;
    private ImageView16by9 mSingleImageView;
    private TextView mTextView;
    private TextView mSingleImageMetaText;

    // Target
    private ShareTargetsAdapter mAdapter;

    // Share data
    @NonNull private ArrayList<Share> mReadyShares = new ArrayList<>();
    @Nullable private String mReadyShareText = null;

    private enum UiMode {
        Text,
        SingleImage,
        Images,
        SingleFile,
        Files,
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final CreateSessionResult result = SessionConnector.get().createActivityWithSession(this);
        if (result.state == CreateSessionState.NO_CREDENTIALS) return;

        setContentView(R.layout.activity_share_receiver);

        Ui.prepareActivityForTaskManager(this);
        Ui.setupActionbar(this, false);
        Ui.setColorStatusBarArrangeHeader(this);

        setupUi();

        if (result.state == CreateSessionState.CREATING) {
            RuntimeAssertion.require(result.task != null);

            // This might take long since app might not run already
            result.task.waitUntilDone();
        }

        GcmConnector.get().fetchAndRegisterToken(this);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (action.equals(Intent.ACTION_SEND) && type != null) {
            if (type.equals("text/plain")) {
                String sharedText = prepareShareText(intent.getStringExtra(Intent.EXTRA_TEXT));
                prepareUi(UiMode.Text, null);
                handleSendText(sharedText);
            } else if (type.startsWith("image/")) {
                Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                prepareUi(UiMode.SingleImage, null);
                handleSendSingleImage(imageUri);
            } else {
                Uri fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                prepareUi(UiMode.SingleFile, null);
                handleSendFiles(Collections.singletonList(fileUri));
            }
        } else if (action.equals(Intent.ACTION_SEND_MULTIPLE) && type != null) {
            if (type.startsWith("image/")) {
                final List<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (imageUris.size() == 1) {
                    prepareUi(UiMode.SingleImage, null);
                    handleSendSingleImage(imageUris.get(0));
                } else {
                    prepareUi(UiMode.Images, imageUris.size());
                    handleSendMultipleImages(imageUris);
                }
            } else {
                final List<Uri> fileUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (fileUris.size() == 1) {
                    prepareUi(UiMode.SingleFile, null);
                    handleSendFiles(Collections.singletonList(fileUris.get(0)));
                } else {
                    prepareUi(UiMode.Files, fileUris.size());
                    handleSendFiles(fileUris);
                }
            }
        } else {
            Log.e(TAG, "Invalid intend data");
        }
    }

    @NonNull
    private static String prepareShareText(@NonNull final String inText) {
        String out = inText.trim();
        if (out.length() > KulloConstants.SHARE_TEXT_LIMIT) {
            out = out.substring(0, KulloConstants.SHARE_TEXT_LIMIT-1) + "…";
        }
        return out;
    }

    private void prepareUi(UiMode mode, @Nullable final Integer itemsCount) {
        switch (mode) {
            case Text:
                setTitle(getString(R.string.sharereceiver_title_share_text));
                mPreviewContainerText.setVisibility(View.VISIBLE);
                break;
            case SingleImage:
                setTitle(getString(R.string.sharereceiver_title_share_single_image));
                // Hide container until preview is available
                mPreviewContainerSingleImage.setVisibility(View.GONE);
                break;
            case Images:
                setTitle(String.format(getString(R.string.sharereceiver_title_share_images), itemsCount));
                mPreviewContainerMultipleImages.setVisibility(View.VISIBLE);
                break;
            case SingleFile:
                setTitle(getString(R.string.sharereceiver_title_share_single_file));
                mPreviewContainerFiles.setVisibility(View.VISIBLE);
                break;
            case Files:
                setTitle(String.format(getString(R.string.sharereceiver_title_share_files), itemsCount));
                mPreviewContainerFiles.setVisibility(View.VISIBLE);
                break;
            default:
                RuntimeAssertion.fail("Unhandled mode value");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadConversationsList();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "Request: " + requestCode + " Result: " + resultCode);

        switch (requestCode) {
            case KulloConstants.REQUEST_CODE_SEND_MESSAGE_WITH_SHARE:
                if (resultCode == RESULT_OK) {
                    // Done sending share
                    setResult(RESULT_OK);
                    finish();
                } else {
                    // Cancel sharing
                    setResult(RESULT_CANCELED);
                    finish();
                }
                break;
            default:
                RuntimeAssertion.fail("Unhandled request code: " + requestCode);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: // back button
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupUi() {
        mReadFilesProgressIndicator = (MaterialProgressBar) findViewById(R.id.read_files_progress_indicator);
        RuntimeAssertion.require(mReadFilesProgressIndicator != null);
        mPreviewContainerMultipleImages = (RecyclerView) findViewById(R.id.preview_container_multiple_images);
        RuntimeAssertion.require(mPreviewContainerMultipleImages != null);
        mPreviewContainerFiles = (RecyclerView) findViewById(R.id.preview_container_files);
        RuntimeAssertion.require(mPreviewContainerFiles != null);
        mPreviewContainerSingleImage = findViewById(R.id.preview_container_single_image);
        RuntimeAssertion.require(mPreviewContainerSingleImage != null);
        mPreviewContainerText = (CardView) findViewById(R.id.card_text);
        RuntimeAssertion.require(mPreviewContainerText != null);
        mTextView = (TextView) findViewById(R.id.text_view);
        RuntimeAssertion.require(mTextView != null);
        mSingleImageView = (ImageView16by9) findViewById(R.id.single_image_view);
        RuntimeAssertion.require(mSingleImageView != null);
        mSingleImageMetaText = (TextView) findViewById(R.id.single_image_meta_text);
        RuntimeAssertion.require(mSingleImageMetaText != null);

        RecyclerView conversationsRecyclerView = (RecyclerView) findViewById(R.id.conversations_recycler_view);
        RuntimeAssertion.require(conversationsRecyclerView != null);

        // Disable to fix initial scroll position of surrounding NestedScrollView
        // See http://stackoverflow.com/a/38126982
        conversationsRecyclerView.setFocusable(false);

        // Disable nested scrolling to ensure the containing NestedScrollView scrolls properly
        conversationsRecyclerView.setNestedScrollingEnabled(false);

        final LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        conversationsRecyclerView.setLayoutManager(llm);

        conversationsRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(conversationsRecyclerView) {
            @Override
            public void onItemClick(View view, int position) {
                long conversationId = mAdapter.getItem(position);

                Intent intent = new Intent(ShareReceiverActivity.this, ComposeActivity.class);
                intent.putExtra(KulloConstants.CONVERSATION_ID, conversationId);
                if (mReadyShareText != null) {
                    intent.putExtra(KulloConstants.CONVERSATION_ADD_TEXT, mReadyShareText);
                } else {
                    intent.putParcelableArrayListExtra(KulloConstants.CONVERSATION_ADD_ATTACHMENTS,
                            getShareUris(mReadyShares));
                }
                startActivityForResult(intent, KulloConstants.REQUEST_CODE_SEND_MESSAGE_WITH_SHARE);

                Log.d(TAG, "Clicked pos:  " + position);
            }

            @Override
            public void onItemLongPress(View view, int position) {
            }
        });

        mAdapter = new ShareTargetsAdapter(this);
        conversationsRecyclerView.setAdapter(mAdapter);

        // Add decoration for dividers between list items
        int dividerLeftMargin = getResources().getDimensionPixelSize(R.dimen.md_additions_list_divider_margin_left);
        conversationsRecyclerView.addItemDecoration(new DividerDecoration(this, dividerLeftMargin));
    }

    @NonNull
    private ArrayList<Uri> getShareUris(@NonNull final List<Share> shares) {
        ArrayList<Uri> out = new ArrayList<>(shares.size());
        for (Share share : shares) {
            out.add(share.uri);
        }
        return out;
    }

    private void reloadConversationsList() {
        RuntimeAssertion.require(mAdapter != null);
        List<Long> conversationIds = SessionConnector.get().getAllConversationIdsSorted();
        mAdapter.replaceAll(conversationIds);
    }

    private void handleSendText(@NonNull final String sharedText) {
        Log.d(TAG, "Got text: " + sharedText);
        mReadyShareText = sharedText;
        mTextView.setText(sharedText);
    }

    private void handleSendSingleImage(@NonNull final Uri imageUri) {
        handleFiles(Collections.singletonList(imageUri), true, new Runnable() {
            @Override
            public void run() {
                // Get data
                Share share = mReadyShares.get(0);
                final String imageMetaText = String.format("%s (%s)",
                        share.filename, Formatting.filesizeHuman(share.size));

                // Set data
                mSingleImageView.setImageBitmap(share.preview);
                mSingleImageMetaText.setText(imageMetaText);

                // Layout
                mPreviewContainerSingleImage.setVisibility(View.VISIBLE);
            }
        });
    }

    private void handleSendMultipleImages(@NonNull final List<Uri> imageUris) {
        setupImagesPreviewAdapter();
        handleFiles(imageUris, false, new Runnable() {
            @Override
            public void run() {
                for (Share share : mReadyShares) {
                    mImagesPreviewAdapter.add(share);
                }
            }
        });
    }

    private void handleSendFiles(@NonNull final List<Uri> fileUris) {
        setupFilesAdapter();
        handleFiles(fileUris, null, new Runnable() {
            @Override
            public void run() {
                for (Share share : mReadyShares) {
                    mFilesAdapter.add(share);
                }
            }
        });
    }

    private void setupImagesPreviewAdapter() {
        // must not be set up before
        RuntimeAssertion.require(mImagesPreviewAdapter == null);

        final LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.HORIZONTAL);
        mPreviewContainerMultipleImages.setLayoutManager(llm);

        mImagesPreviewAdapter = new ImagesPreviewAdapter();
        mPreviewContainerMultipleImages.setAdapter(mImagesPreviewAdapter);
    }

    private void setupFilesAdapter() {
        // must not be set up before
        RuntimeAssertion.require(mFilesAdapter == null);

        final LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.HORIZONTAL);
        mPreviewContainerFiles.setLayoutManager(llm);

        mFilesAdapter = new FilesAdapter();
        mPreviewContainerFiles.setAdapter(mFilesAdapter);
    }

    private void handleFiles(@NonNull final List<Uri> fileUris,
                             @Nullable final Boolean highResolutionPreviews,
                             final Runnable successCallback) {
        Log.d(TAG, "Handle incoming shares: " + fileUris);

        new AsyncTask<Uri, Void, List<Share>>() {
            int mReadErrorCount = 0;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mReadFilesProgressIndicator.setVisibility(View.VISIBLE);
                Log.d(TAG, "Start copying files ...");
            }

            @Override
            protected List<Share> doInBackground(Uri... fileList) {
                List<Share> out = new ArrayList<>();

                for (int fileIndex = 0; fileIndex < fileList.length; fileIndex++) {
                    final Uri file = fileList[fileIndex];

                    final String subfolderName = "share_" + fileIndex;
                    final File targetDir = ((KulloApplication) getApplication()).cacheDir(CacheType.ReceivedShares, subfolderName);
                    final String targetFilename = UriHelpers.getFilename(ShareReceiverActivity.this, file, "tmp.tmp");
                    final File targetFile = new File(targetDir, targetFilename);
                    if (!StreamCopy.copyToPath(ShareReceiverActivity.this, file, targetFile.getAbsolutePath())) {
                        Log.e(TAG, "Error copying file");
                        mReadErrorCount++;
                    } else {
                        Share prev = new Share();
                        prev.uri = Uri.fromFile(targetFile);
                        prev.filename = targetFilename;
                        prev.size = targetFile.length();
                        if (highResolutionPreviews != null) {
                            // Split both cases into separate code branches to debug OOM crashes
                            if (highResolutionPreviews) {
                                prev.preview = AvatarUtils.loadBitmap(ShareReceiverActivity.this,
                                        prev.uri, 6_000_000, 2048);
                            } else {
                                prev.preview = AvatarUtils.loadBitmap(ShareReceiverActivity.this,
                                        prev.uri, 3_000_000, 2048);
                            }
                        }
                        out.add(prev);
                    }
                }
                return out;
            }

            @Override
            protected void onPostExecute(List<Share> shares) {
                super.onPostExecute(shares);
                Log.d(TAG, "Done copying files");

                mReadyShares = new ArrayList<>(shares);

                mReadFilesProgressIndicator.setVisibility(View.GONE);

                if (mReadErrorCount != 0) {
                    final String errorText = (mReadErrorCount == 1)
                            ? getString(R.string.sharereceiver_error_copy_file)
                            : String.format(getString(R.string.sharereceiver_error_copy_files), mReadErrorCount);
                    Toast.makeText(ShareReceiverActivity.this, errorText, Toast.LENGTH_LONG).show();
                }

                if (mReadyShares.isEmpty()) {
                    // Go back to sharing app (while error toast is still visible)
                    ShareReceiverActivity.this.finish();
                    return;
                }

                if (successCallback != null) {
                    successCallback.run();
                }
            }
        }.execute(fileUris.toArray(new Uri[fileUris.size()]));
    }
}
