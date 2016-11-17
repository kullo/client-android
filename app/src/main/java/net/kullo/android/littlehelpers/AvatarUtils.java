/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import net.kullo.android.R;
import net.kullo.javautils.RuntimeAssertion;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

public class AvatarUtils {
    private static final String TAG = "AvatarUtils";

    // Suppress default constructor for noninstantiability
    private AvatarUtils() {
        throw new AssertionError();
    }

    @Nullable
    public static Bitmap avatarToBitmap(byte[] encodeByte) {
        if (encodeByte == null || encodeByte.length == 0) {
            return null;
        }

        try {
            return BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
        } catch(Exception e) {
            e.getMessage();
            return null;
        }
    }

    public static Bitmap getSenderThumbnailFromInitials(Context context, String initials) {
        int avatarSizePx = context.getResources().getDimensionPixelSize(R.dimen.avatar_bitmap_initials_size);
        int textSizePx = context.getResources().getDimensionPixelSize(R.dimen.avatar_bitmap_initials_text_size);

        Bitmap bitmap = Bitmap.createBitmap(avatarSizePx, avatarSizePx, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(context.getResources().getColor(R.color.kulloAvatarBitmapBGColor));

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(context.getResources().getColor(R.color.kulloAvatarBitmapTextColor));
        paint.setTextSize(textSizePx);

        // draw text to the Canvas center
        Rect bounds = new Rect();
        paint.getTextBounds(initials, 0, initials.length(), bounds);
        int x = (bitmap.getWidth() - bounds.width()) / 2;
        int y = (bitmap.getHeight() + bounds.height()) / 2;

        canvas.drawText(initials, x, y, paint);

        return bitmap;
    }

    public static byte[] bitmapToJpegBinaryWithDownsamplingQualityAndMaxByteArraySize(Bitmap bitmap, int quality, int maxByteArraySize) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        byte[] byteArray = stream.toByteArray();

        if (byteArray.length > maxByteArraySize) {
            return bitmapToJpegBinaryWithDownsamplingQualityAndMaxByteArraySize(bitmap, quality - KulloConstants.AVATAR_QUALITY_DOWNSAMPLING_STEPS, KulloConstants.AVATAR_MAX_SIZE);
        } else {
            return byteArray;
        }
    }

    public static Bitmap resizeBitmapWithShorterSideResizedTo(int shorterSideLength, Bitmap bitmap) {

        //get the shorter side
        float sizeShorterSide = Math.min(bitmap.getWidth(), bitmap.getHeight());

        float scale = (float)shorterSideLength / sizeShorterSide;

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

    }

    public static Bitmap resizeBitmapWithLongerSideLimitedTo(int limitSideLength, Bitmap bitmap) {
        float sizeLongerSide = Math.max(bitmap.getWidth(), bitmap.getHeight());

        if (sizeLongerSide > limitSideLength) {
            float scale = (float)limitSideLength / sizeLongerSide;

            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);

            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } else {
            return bitmap;
        }

    }

    public static Bitmap cropFromCenterForThumbnail(Bitmap bitmap, int dimension) {
        return ThumbnailUtils.extractThumbnail(bitmap, dimension, dimension);
    }

    @Nullable
    public static Bitmap combine(List<Bitmap> senderAvatars, int outPixelSize) {
        final int count = senderAvatars.size();

        if (count == 0) return null;
        if (count == 1) return senderAvatars.get(0);

        Bitmap out = Bitmap.createBitmap(outPixelSize, outPixelSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);

        if (count == 2) {
            Bitmap b1 = senderAvatars.get(0);
            Bitmap b2 = senderAvatars.get(1);
            int b1Size = b1.getWidth();
            int b2Size = b2.getWidth();
            Rect src1 = new Rect(b1Size/4, 0, (b1Size*3)/4, b1Size);
            Rect src2 = new Rect(b2Size/4, 0, (b2Size*3)/4, b2Size);
            Rect dstLeft = new Rect(0, 0, outPixelSize/2, outPixelSize);
            Rect dstRight = new Rect(outPixelSize/2, 0, outPixelSize, outPixelSize);
            canvas.drawBitmap(b1, src1, dstLeft, null);
            canvas.drawBitmap(b2, src2, dstRight, null);
        } else if (count == 3) {
            Bitmap b1 = senderAvatars.get(0);
            Bitmap b2 = senderAvatars.get(1);
            Bitmap b3 = senderAvatars.get(2);
            int b1Size = b1.getWidth();
            int b2Size = b2.getWidth();
            int b3Size = b3.getWidth();
            Rect src1 = new Rect(b1Size/4, 0, (b1Size*3)/4, b1Size);
            Rect src2 = new Rect(0, 0, b2Size, b2Size);
            Rect src3 = new Rect(0, 0, b3Size, b3Size);
            Rect dstLeft = new Rect(0, 0, outPixelSize/2, outPixelSize);
            Rect dstRightTop = new Rect(outPixelSize/2, 0, outPixelSize, outPixelSize/2);
            Rect dstRightBottom = new Rect(outPixelSize/2, outPixelSize/2, outPixelSize, outPixelSize);
            canvas.drawBitmap(b1, src1, dstLeft, null);
            canvas.drawBitmap(b2, src2, dstRightTop, null);
            canvas.drawBitmap(b3, src3, dstRightBottom, null);
        } else if (count >= 4) {
            Bitmap b1 = senderAvatars.get(0);
            Bitmap b2 = senderAvatars.get(1);
            Bitmap b3 = senderAvatars.get(2);
            Bitmap b4 = senderAvatars.get(3);
            int b1Size = b1.getWidth();
            int b2Size = b2.getWidth();
            int b3Size = b3.getWidth();
            int b4Size = b4.getWidth();
            Rect src1 = new Rect(0, 0, b1Size, b1Size);
            Rect src2 = new Rect(0, 0, b2Size, b2Size);
            Rect src3 = new Rect(0, 0, b3Size, b3Size);
            Rect src4 = new Rect(0, 0, b4Size, b4Size);
            Rect dstLeftTop = new Rect(0, 0, outPixelSize/2, outPixelSize/2);
            Rect dstLeftBottom = new Rect(0, outPixelSize/2, outPixelSize/2, outPixelSize);
            Rect dstRightTop = new Rect(outPixelSize/2, 0, outPixelSize, outPixelSize/2);
            Rect dstRightBottom = new Rect(outPixelSize/2, outPixelSize/2, outPixelSize, outPixelSize);
            canvas.drawBitmap(b1, src1, dstLeftTop, null);
            canvas.drawBitmap(b2, src2, dstRightTop, null);
            canvas.drawBitmap(b3, src3, dstLeftBottom, null);
            canvas.drawBitmap(b4, src4, dstRightBottom, null);
        }

        // TODO: handle 5+ avatars

        return out;
    }

    public static int sampleSize(double scaleFactorUpperLimit) {
        double sampleSizeDouble = 1/scaleFactorUpperLimit;

        // round up to power of two
        double logBase2OfSampleSize = Math.log(sampleSizeDouble) / Math.log(2);
        return (int) Math.pow(2, Math.ceil(logBase2OfSampleSize));
    }

    public static double scalingFactorOneDimension(int originalWidth, int originalHeight, int maxPixelCount) {
        int originalPixelCount = originalWidth*originalHeight;

        double pixelsFactor = Math.min(1.0, (double) maxPixelCount/originalPixelCount);

        return Math.sqrt(pixelsFactor);
    }

    public static Bitmap loadBitmap(@NonNull final Context context,
                                    @NonNull final Uri bitmapSource,
                                    final int downsamplingMaxPixelCount,
                                    final int maxTextureSize) {
        final int sampleSize = sampleSize(context, bitmapSource, downsamplingMaxPixelCount);
        Log.d(TAG, "Sample size: " + sampleSize);

        final Bitmap selectedBitmap;
        InputStream input = null;
        try {
            input = context.getContentResolver().openInputStream(bitmapSource);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            RuntimeAssertion.fail("Failed to read bitmap from source");
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        selectedBitmap = BitmapFactory.decodeStream(input, null, options);

        Log.d(TAG, "Bitmap in memory: "
                + selectedBitmap.getWidth() + "x" + selectedBitmap.getHeight() + "px"
                + " (" + selectedBitmap.getByteCount() + " bytes)");

        return resizeBitmapWithLongerSideLimitedTo(
                maxTextureSize, selectedBitmap);
    }

    private static int sampleSize(Context context, final Uri bitmapSource, int maxPixelCount) {
        try {
            InputStream input = context.getContentResolver().openInputStream(bitmapSource);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, options);

            Log.d(TAG, options.outWidth + "x" + options.outHeight);

            double scalingFactor = scalingFactorOneDimension(
                    options.outWidth, options.outHeight, maxPixelCount);
            return sampleSize(scalingFactor);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
    }
}
