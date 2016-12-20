/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;

import net.kullo.javautils.RuntimeAssertion;
import net.kullo.reduce.Reduce;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

public class Images {
    public static final Collection<String> SCALABLE_TYPES = Collections.singleton(
            "image/jpeg"
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
        Reduce.scale(filePath.getAbsolutePath(), filePath.getAbsolutePath(),
                outDimensions.width, outDimensions.height);
    }

    @NonNull
    static Size destinationSizeForPixelLimit(@NonNull Size inDimensions, int pixelLimit) {
        int[] out = Reduce.destinationSizeForPixelLimit(
                inDimensions.width, inDimensions.height, pixelLimit);
        return new Size(out[0], out[1]);
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
}
