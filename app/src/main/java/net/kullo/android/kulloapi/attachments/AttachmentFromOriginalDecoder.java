/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.kulloapi.attachments;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.bumptech.glide.load.resource.bytes.BytesResource;

import net.kullo.android.littlehelpers.Images;

import java.io.IOException;

public class AttachmentFromOriginalDecoder implements ResourceDecoder<BytesResource, Bitmap> {

    @NonNull private final Context mGlideContext;

    public AttachmentFromOriginalDecoder(@NonNull final Context glideContext) {
        mGlideContext = glideContext;
    }

    @Override
    public Resource<Bitmap> decode(BytesResource source, int width, int height) throws IOException {
        final Bitmap bitmap = Images.makeThumbnail(source.get(), width, height);
        return BitmapResource.obtain(bitmap, Glide.get(mGlideContext).getBitmapPool());
    }

    @Override
    public String getId() {
        return AttachmentFromOriginalDecoder.class.getCanonicalName();
    }
}
