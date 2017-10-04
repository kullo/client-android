/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.reduce;

import android.support.annotation.CheckResult;

public class Reduce {
    private static int ALGORITHM_SCALE = 0;
    private static int ALGORITHM_RESIZE = 1;

    static {
        System.loadLibrary("c++_shared");
        System.loadLibrary("reduce-jni");
    }

    public static Size destinationSizeForPixelLimit(Size srcSize, int pixelLimit) {
        int[] size = jniDestinationSizeForPixelLimit(srcSize.width, srcSize.height, pixelLimit);
        return new Size(size[0], size[1]);
    }

    @CheckResult
    public static int scale(
            final String srcFilepath,
            final String dstFilepath,
            final Size dstSize) {
        return jniScale(srcFilepath, dstFilepath, dstSize.width, dstSize.height, ALGORITHM_SCALE);
    }

    @CheckResult
    public static int resize(
            final String srcFilepath,
            final String dstFilepath,
            final Size dstSize) {
        return jniScale(srcFilepath, dstFilepath, dstSize.width, dstSize.height, ALGORITHM_RESIZE);
    }

    native private static int[] jniDestinationSizeForPixelLimit(
            int srcWidth,
            int srcHeight,
            int pixelLimit);

    @CheckResult
    native private static int jniScale(
            final String srcFilepath,
            final String dstFilepath,
            int with,
            int height,
            int algorithmId);
}
