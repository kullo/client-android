/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.screens.sharereceiver;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.Nullable;

public class Share {
    public Uri cacheUri;
    public long size;
    public String filename;
    @Nullable public Uri previewUri;
}
