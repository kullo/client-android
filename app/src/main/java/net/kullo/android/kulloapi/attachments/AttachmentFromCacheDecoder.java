/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.kulloapi.attachments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;

import java.io.File;
import java.io.IOException;

public class AttachmentFromCacheDecoder implements ResourceDecoder<File, Bitmap> {
    @NonNull
    private final Context mGlideContext;

    public AttachmentFromCacheDecoder(@NonNull final Context glideContext) {
        mGlideContext = glideContext;
    }

    @Override
    public Resource<Bitmap> decode(File source, int width, int height) throws IOException {
        // Assume source has the correct size
        final Bitmap bitmap = BitmapFactory.decodeFile(source.getAbsolutePath());
        return BitmapResource.obtain(bitmap, Glide.get(mGlideContext).getBitmapPool());
    }

    @Override
    public String getId() {
        return AttachmentFromCacheDecoder.class.getCanonicalName();
    }
}
