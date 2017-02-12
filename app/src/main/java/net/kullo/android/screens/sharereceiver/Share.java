/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
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
