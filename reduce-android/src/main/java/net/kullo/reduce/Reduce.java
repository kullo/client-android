/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.reduce;

public class Reduce {
    private static int ALGORITHM_SCALE = 0;
    private static int ALGORITHM_RESIZE = 1;

    static {
        System.loadLibrary("crystax");
        System.loadLibrary("reduce-jni");
    }

    public static Size destinationSizeForPixelLimit(Size srcSize, int pixelLimit) {
        int[] size = jniDestinationSizeForPixelLimit(srcSize.width, srcSize.height, pixelLimit);
        return new Size(size[0], size[1]);
    }

    public static void scale(
            final String srcFilepath,
            final String dstFilepath,
            final Size dstSize) {
        jniScale(srcFilepath, dstFilepath, dstSize.width, dstSize.height, ALGORITHM_SCALE);
    }

    public static void resize(
            final String srcFilepath,
            final String dstFilepath,
            final Size dstSize) {
        jniScale(srcFilepath, dstFilepath, dstSize.width, dstSize.height, ALGORITHM_RESIZE);
    }

    native private static int[] jniDestinationSizeForPixelLimit(
            int srcWidth,
            int srcHeight,
            int pixelLimit);

    native private static void jniScale(
            final String srcFilepath,
            final String dstFilepath,
            int with,
            int height,
            int algorithmId);
}
