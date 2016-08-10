/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.isseiaoki.simplecropview.CropImageView;

import net.kullo.android.R;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.AvatarUtils;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.littlehelpers.Ui;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class CropImageActivity extends AppCompatActivity {
    public static final String INPUT_METHOD = "InputMethod";
    public static final String CAMERA_INPUT = "Camera";
    public static final String FILE_INPUT = "Filesystem";
    public static final String BITMAP_INPUT_URI = "FileUri";

    private CropImageView mCropImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_image);

        Ui.prepareActivityForTaskManager(this);
        Ui.setupActionbar(this);
        Ui.setColorStatusBarArrangeHeader(this);

        mCropImageView = (CropImageView)findViewById(R.id.cropImageView);
        loadInputBitmap();
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

    private void loadInputBitmap() {
        final Intent intent = getIntent();
        final Uri fileUri = Uri.parse(intent.getStringExtra(BITMAP_INPUT_URI));

        Bitmap selectedBitmap;
        try {
            InputStream input = getContentResolver().openInputStream(fileUri);
            selectedBitmap = BitmapFactory.decodeStream(input);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        // Limit maximum size of bitmap. Otherwise we risk exception for out of memory
        selectedBitmap = AvatarUtils.resizeBitmapWithLongerSideLimitedTo(KulloConstants.AVATAR_MAX_ALLOWED_SIDE, selectedBitmap);

        mCropImageView.setImageBitmap(selectedBitmap);
    }

    private void storeCroppedBitmap() {
        Bitmap bitmap = mCropImageView.getCroppedBitmap();
        Bitmap resizedBitmap = AvatarUtils.resizeBitmapWithShorterSideResizedTo(KulloConstants.AVATAR_DIMENSION, bitmap);
        Bitmap sourceAvatar = AvatarUtils.cropFromCenterForThumbnail(resizedBitmap, KulloConstants.AVATAR_DIMENSION);
        byte[] avatar = AvatarUtils.bitmapToJpegBinaryWithDownsamplingQualityAndMaxByteArraySize(
                    sourceAvatar, KulloConstants.AVATAR_BEST_QUALITY, KulloConstants.AVATAR_MAX_SIZE);
        SessionConnector.get().setClientAvatar(avatar);
        SessionConnector.get().setClientAvatarMimeType("image/jpeg");
    }

    private void rotateImage() {
        mCropImageView.rotateImage(CropImageView.RotateDegrees.ROTATE_90D);
    }
}
