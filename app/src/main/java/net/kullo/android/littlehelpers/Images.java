/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.littlehelpers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import net.kullo.javautils.RuntimeAssertion;
import net.kullo.reduce.Reduce;
import net.kullo.reduce.Size;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class Images {
    private static final String TAG = Images.class.getSimpleName();

    public static final Collection<String> SCALABLE_TYPES = Collections.singleton(
        "image/jpeg"
    );
    public static final Collection<String> THUMBNAILABLE_TYPES = Arrays.asList(
        "image/jpeg",
        "image/png"
    );
    public static int MEGA_PIXEL = 1_000_000;

    public static void scaleDownInplace(@NonNull final File filePath, int pixelLimit) {
        Size inDimensions = getImageDimensions(Uri.fromFile(filePath));
        if (inDimensions.width*inDimensions.height <= pixelLimit) {
            // images does not exceed pixel limit => do nothing
            return;
        }

        Size outDimensions = destinationSizeForPixelLimit(inDimensions, pixelLimit);
        RuntimeAssertion.require(outDimensions.width*outDimensions.height <= pixelLimit,
                "Dimensions exceed limit (w=" + outDimensions.width + " h=" + outDimensions.height + ")");
        Reduce.scale(filePath.getAbsolutePath(), filePath.getAbsolutePath(), outDimensions);
    }

    @NonNull
    static Size destinationSizeForPixelLimit(@NonNull Size inDimensions, int pixelLimit) {
        return Reduce.destinationSizeForPixelLimit(inDimensions, pixelLimit);
    }

    @NonNull
    private static Size getImageDimensions(@NonNull final Uri fileUri) {
        // only file:// URIs supported for now
        RuntimeAssertion.require(
                fileUri.getScheme().equals("file")
        );

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        // Decode meta without reading pixels
        RuntimeAssertion.require(
                BitmapFactory.decodeFile(fileUri.getPath(), options) == null
        );

        return new Size(options.outWidth, options.outHeight);
    }

    @NonNull
    private static Size getImageDimensions(@NonNull final byte[] data) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        // Decode meta without reading pixels
        RuntimeAssertion.require(
            BitmapFactory.decodeByteArray(data, 0, data.length, options) == null
        );

        return new Size(options.outWidth, options.outHeight);
    }

    public static Bitmap makeThumbnail(byte[] data, int width, int height) {
        RuntimeAssertion.require(data.length > 1);
        RuntimeAssertion.require(width > 0);
        RuntimeAssertion.require(height > 0);

        Size originalSize = getImageDimensions(data);

        int samplingSizeHorizontal = getSamplingSize(originalSize.width, width);
        int samplingSizeVertical = getSamplingSize(originalSize.height, height);
        Log.d(TAG, "Horizontal scale from " + originalSize.width + " to " + width
            + " using sampling size " + samplingSizeHorizontal);
        Log.d(TAG, "Vertical scale from " + originalSize.height + " to " + height
            + " using sampling size " + samplingSizeVertical);

        int samplingSize = Math.min(samplingSizeHorizontal, samplingSizeVertical);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = samplingSize;
        options.inScaled = false;
        Bitmap decoded = BitmapFactory.decodeByteArray(data, 0, data.length, options);

        return ThumbnailUtils.extractThumbnail(decoded, width, height);
    }

    private static int getSamplingSize(int originalSize, int targetSize) {
        // when resizing to 100 px we want to have at least 200 px available
        float PIXEL_OVERHEAD_FACTOR = 2.0f;

        int samplingSize = 1;
        while ((originalSize / samplingSize) > PIXEL_OVERHEAD_FACTOR*targetSize) {
            samplingSize *= 2;
        }
        return samplingSize;
    }
}
