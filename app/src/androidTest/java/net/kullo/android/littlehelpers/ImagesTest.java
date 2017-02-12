/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

import android.support.test.runner.AndroidJUnit4;

import net.kullo.reduce.Size;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class ImagesTest {
    @Test
    public void destinationSizeForPixelLimit() {
        {
            final Size original = new Size(100, 50);
            // hit limit
            assertEquals(new Size(100, 50), Images.destinationSizeForPixelLimit(original, 5000));
            assertEquals(new Size(40, 20), Images.destinationSizeForPixelLimit(original, 800));
            assertEquals(new Size(0, 0), Images.destinationSizeForPixelLimit(original, 0));
            assertEquals(new Size(200, 100), Images.destinationSizeForPixelLimit(original, 20_000));

            // below limit
            assertEquals(new Size(100, 50), Images.destinationSizeForPixelLimit(original, 5001));
            assertEquals(new Size(100, 50), Images.destinationSizeForPixelLimit(original, 5040));
            assertEquals(new Size(40, 20), Images.destinationSizeForPixelLimit(original, 810));
            assertEquals(new Size(0, 0), Images.destinationSizeForPixelLimit(original, 1));
            assertEquals(new Size(200, 100), Images.destinationSizeForPixelLimit(original, 20_050));
        }
        {
            // 3:2 image might cause more rounding issues
            final Size original = new Size(600, 400);
            // hit limit
            assertEquals(new Size(300, 200), Images.destinationSizeForPixelLimit(original, 60_000));
            assertEquals(new Size(60, 40), Images.destinationSizeForPixelLimit(original, 2400));
            assertEquals(new Size(0, 0), Images.destinationSizeForPixelLimit(original, 0));
            assertEquals(new Size(900, 600), Images.destinationSizeForPixelLimit(original, 540_000));

            // below limit
            assertEquals(new Size(300, 200), Images.destinationSizeForPixelLimit(original, 60100));
            assertEquals(new Size(60, 40), Images.destinationSizeForPixelLimit(original, 2500));
            assertEquals(new Size(0, 0), Images.destinationSizeForPixelLimit(original, 1));
            assertEquals(new Size(900, 600), Images.destinationSizeForPixelLimit(original, 540_100));
        }
        {
            // Read world problematic image (ratio 4:3 or 1,333333333)
            final Size original = new Size(3264, 2448);

            // Image ratio is more important than maximal size.
            // 866x1155 exceeds limit (by 230)
            // 866x1154 changes ratio (ratio 1,33256351; 99,94%)
            // 865x1153 is good (ratio 1,332947977, 99,97%)
            assertEquals(new Size(1153, 865), Images.destinationSizeForPixelLimit(original, 1_000_000));
        }
    }
}
