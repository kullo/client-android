/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShareReceiverActivity extends AppCompatActivity {
    private static String TAG = "ShareReceiverActivity";

    // Preview
    private FilesAdapter mFilesAdapter;
    private ImagesPreviewAdapter mImagesPreviewAdapter;

    private View mMainScrollView;
    private View mReadFilesLoadingIndicator;
    private View mGeneratePreviewsLoadingIndicator;
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
        mReadFilesLoadingIndicator = findViewById(R.id.read_files_loading_indicator);
        RuntimeAssertion.require(mReadFilesLoadingIndicator != null);
        mMainScrollView = findViewById(R.id.main_scroll_view);
        RuntimeAssertion.require(mMainScrollView != null);

        mGeneratePreviewsLoadingIndicator = findViewById(R.id.generate_previews_loading_indicator);
        RuntimeAssertion.require(mGeneratePreviewsLoadingIndicator != null);
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
            out.add(share.cacheUri);
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
        showMainScrollView();
    }

    private void handleSendSingleImage(@NonNull final Uri imageUri) {
        readFiles(Collections.singletonList(imageUri), new Runnable() {
            @Override
            public void run() {
                showMainScrollView();
                showGeneratePreviewsLoadingIndicator();

                generatePreviews(true, new Runnable() {
                    @Override
                    public void run() {
                        hideGeneratePreviewsLoadingIndicator();

                        Share share = mReadyShares.get(0);

                        // Get image meta
                        final String imageMetaText = String.format("%s (%s)",
                                share.filename, Formatting.filesizeHuman(share.size));
                        mSingleImageMetaText.setText(imageMetaText);

                        // Set preview
                        mSingleImageView.setImageURI(share.previewUri);

                        // Layout
                        mPreviewContainerSingleImage.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    private void handleSendMultipleImages(@NonNull final List<Uri> imageUris) {
        setupImagesPreviewAdapter();
        readFiles(imageUris, new Runnable() {
            @Override
            public void run() {
                showMainScrollView();
                showGeneratePreviewsLoadingIndicator();

                generatePreviews(false, new Runnable() {
                    @Override
                    public void run() {
                        hideGeneratePreviewsLoadingIndicator();

                        for (Share share : mReadyShares) {
                            mImagesPreviewAdapter.add(share);
                        }
                    }
                });
            }
        });
    }

    private void handleSendFiles(@NonNull final List<Uri> fileUris) {
        setupFilesAdapter();
        readFiles(fileUris, new Runnable() {
            @Override
            public void run() {
                showMainScrollView();

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

        mImagesPreviewAdapter = new ImagesPreviewAdapter(this);
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

    private void readFiles(@NonNull final List<Uri> fileUris,
                           @Nullable final Runnable successCallback) {
        Log.d(TAG, "Handle incoming shares: " + fileUris);

        new AsyncTask<Uri, Void, List<Share>>() {
            int mReadErrorCount = 0;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
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
                        Share share = new Share();
                        share.cacheUri = Uri.fromFile(targetFile);
                        share.filename = targetFilename;
                        share.size = targetFile.length();
                        out.add(share);
                    }
                }
                return out;
            }

            @Override
            protected void onPostExecute(List<Share> shares) {
                super.onPostExecute(shares);
                Log.d(TAG, "Done copying files");

                mReadyShares = new ArrayList<>(shares);

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

    private void showMainScrollView() {
        mReadFilesLoadingIndicator.setVisibility(View.GONE);
        mMainScrollView.setVisibility(View.VISIBLE);
    }

    private void showGeneratePreviewsLoadingIndicator() {
        mGeneratePreviewsLoadingIndicator.setVisibility(View.VISIBLE);
    }

    private void hideGeneratePreviewsLoadingIndicator() {
        mGeneratePreviewsLoadingIndicator.setVisibility(View.GONE);
    }

    private void generatePreviews(final boolean highResolutionPreviews,
                                  @Nullable final Runnable successCallback) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                for (int index = 0; index < mReadyShares.size(); ++index) {
                    Log.d(TAG, "Generating preview for share at index " + index + " ...");
                    Share share = mReadyShares.get(index);

                    if (share.size > 1_048_576 /* 1 MiB */) {
                        Bitmap bmp;
                        // Split both cases into separate code branches
                        // to debug OOM crashes
                        if (highResolutionPreviews) {
                            bmp = AvatarUtils.loadBitmap(
                                    ShareReceiverActivity.this,
                                    share.cacheUri, 6_000_000, 2048);
                        } else {
                            bmp = AvatarUtils.loadBitmap(
                                    ShareReceiverActivity.this,
                                    share.cacheUri, 3_000_000, 2048);
                        }

                        try {
                            final File outputDir = ((KulloApplication) getApplication()).cacheDir(CacheType.ReceivedSharePreviews, null);
                            final File outputFile = File.createTempFile("prev", ".jpg", outputDir);
                            final FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                            bmp.compress(Bitmap.CompressFormat.JPEG, 85, fileOutputStream);
                            share.previewUri = Uri.fromFile(outputFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                            // ignore
                        }
                    } else {
                        share.previewUri = share.cacheUri;
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (successCallback != null) {
                    successCallback.run();
                }
            }
        }.execute();
    }
}
