/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

import android.test.AndroidTestCase;

public class AvatarUtilsTest extends AndroidTestCase {

    public void testScalingFactorOneDimension() {
        // limit equals size
        assertEquals(1.0, AvatarUtils.scalingFactorOneDimension(1, 1, 1), 0.001);
        assertEquals(1.0, AvatarUtils.scalingFactorOneDimension(2, 2, 4), 0.001);
        assertEquals(1.0, AvatarUtils.scalingFactorOneDimension(10, 10, 100), 0.001);

        // scale down
        assertEquals(0.5, AvatarUtils.scalingFactorOneDimension(2, 2, 1), 0.001);
        assertEquals(1.0/20.0, AvatarUtils.scalingFactorOneDimension(20, 20, 1), 0.001);
        assertEquals(0.5, AvatarUtils.scalingFactorOneDimension(8, 8, 16), 0.001);
        assertEquals(2.0/3.0, AvatarUtils.scalingFactorOneDimension(3, 3, 4), 0.001);

        // no upscaling
        assertEquals(1.0, AvatarUtils.scalingFactorOneDimension(2, 2, 5), 0.001);
        assertEquals(1.0, AvatarUtils.scalingFactorOneDimension(2, 2, 100), 0.001);
    }

    public void testFactorToSampleSize() {
        assertEquals(1, AvatarUtils.sampleSize(1.0));
        assertEquals(2, AvatarUtils.sampleSize(0.5));
        assertEquals(4, AvatarUtils.sampleSize(0.25));
        assertEquals(8, AvatarUtils.sampleSize(0.125));

        // factor is an upper limit: round up to bigger sample size = smaller image
        assertEquals(2, AvatarUtils.sampleSize(0.99));
        assertEquals(2, AvatarUtils.sampleSize(0.51));

        // round to powers of 2
        assertEquals(4, AvatarUtils.sampleSize(0.49));
    }
}
