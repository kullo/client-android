/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.screens;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.isseiaoki.simplecropview.CropImageView;

import net.kullo.android.R;
import net.kullo.android.application.KulloApplication;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.AvatarUtils;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.littlehelpers.Permissions;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.javautils.RuntimeAssertion;

public class CropImageActivity extends AppCompatActivity {
    public static final String INPUT_METHOD = "InputMethod";
    public static final String CAMERA_INPUT = "Camera";
    public static final String FILE_INPUT = "Filesystem";
    public static final String BITMAP_INPUT_URI = "FileUri";
    private static final String TAG = "CropImageActivity";

    private View mPrepareImageProgressIndicator;
    private CropImageView mCropImageView;
    private Uri mBitmapSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_image);

        Ui.prepareActivityForTaskManager(this);
        Ui.setupActionbar(this);
        Ui.setStatusBarColor(this);

        mPrepareImageProgressIndicator = findViewById(R.id.prepare_image_progress_indicator);
        mCropImageView = (CropImageView)findViewById(R.id.crop_image_view);

        final Intent intent = getIntent();
        mBitmapSource = Uri.parse(intent.getStringExtra(BITMAP_INPUT_URI));
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (Permissions.checkOrRequestReadPermission(this)) {
            loadInputBitmapIntoViewAsync();
        } else {
            Log.d(TAG, "Read permission not yet granted.");
            // Try again in onRequestPermissionsResult()
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case KulloApplication.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadInputBitmapIntoViewAsync();
                } else {
                    setResult(ProfileSettingsActivity.ACTIVITY_RESULT_PERMISSION_DENIED_READ_STORAGE);
                    finish();
                }
                break;
            default:
                RuntimeAssertion.fail("Unhandled request code in onRequestPermissionsResult()");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: // back button
                finish();
                return true;
            case R.id.action_rotate:
                rotateImage();
                return true;
            case R.id.action_finish_crop:
                storeCroppedBitmap();
                setResult(RESULT_OK, getIntent());
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.appbar_menu_crop_image, menu);
        return true;
    }

    private void loadInputBitmapIntoViewAsync() {
        RuntimeAssertion.require(mBitmapSource != null);

        new android.os.AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mPrepareImageProgressIndicator.setVisibility(View.VISIBLE);
            }

            protected Bitmap doInBackground(Void... params) {
                // Resize to fit the minimal GL_MAX_TEXTURE_SIZE: 2048x2048
                // The value of GL_MAX_TEXTURE_SIZE might be higher (e.g. 4096x496 on the Motorola
                // Moto G 2. Gen), but 2048 is always guaranteed and it is not trivial to query the
                // actual value of GL_MAX_TEXTURE_SIZE without an openGL context.
                int maxTextureSize = 2048;
                Log.d(TAG, "Resizing to max texture size " + maxTextureSize);

                return AvatarUtils.loadBitmap(CropImageActivity.this, mBitmapSource,
                        KulloConstants.AVATAR_PREVIEW_MAX_PIXEL_COUNT, maxTextureSize);
            }

            protected void onPostExecute(Bitmap resizedBitmap) {
                Log.d(TAG, "New image size: " + resizedBitmap.getWidth() + "x" + resizedBitmap.getHeight());

                mPrepareImageProgressIndicator.setVisibility(View.INVISIBLE);
                mCropImageView.setImageBitmap(resizedBitmap);
            }
        }.execute();
    }

    private void storeCroppedBitmap() {
        Bitmap bitmap = mCropImageView.getCroppedBitmap();
        Bitmap resizedBitmap = AvatarUtils.resizeBitmapWithShorterSideResizedTo(KulloConstants.AVATAR_DIMENSION, bitmap);
        Bitmap sourceAvatar = AvatarUtils.cropFromCenterForThumbnail(resizedBitmap, KulloConstants.AVATAR_DIMENSION);

        byte[] avatarAsPng = null;
        if (sourceAvatar.hasAlpha()) {
            avatarAsPng = AvatarUtils.encodeAsPng(sourceAvatar, KulloConstants.AVATAR_MAX_SIZE);
        }

        final String mimeType;
        final byte[] avatar;
        if (avatarAsPng != null) {
            avatar = avatarAsPng;
            mimeType = "image/png";
        } else {
            avatar = AvatarUtils.encodeAsJpeg(sourceAvatar, KulloConstants.AVATAR_BEST_QUALITY, KulloConstants.AVATAR_MAX_SIZE);
            mimeType = "image/jpeg";
        }

        SessionConnector.get().setCurrentUserAvatar(avatar);
        SessionConnector.get().setCurrentUserAvatarMimeType(mimeType);
    }

    private void rotateImage() {
        mCropImageView.rotateImage(CropImageView.RotateDegrees.ROTATE_90D);
    }
}
