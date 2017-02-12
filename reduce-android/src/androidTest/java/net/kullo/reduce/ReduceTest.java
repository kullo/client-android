/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.reduce;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.RawRes;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;


@RunWith(AndroidJUnit4.class)
public class ReduceTest {

    @Test
    public void loadLibrary() throws Exception {
        Reduce instance = new Reduce();
    }

    @Test
    public void destinationSizeForPixelLimit() {
        {
            final Size original = new Size(100, 50);
            // hit limit
            assertEquals(new Size(100, 50), Reduce.destinationSizeForPixelLimit(original, 5000));
            assertEquals(new Size(40, 20), Reduce.destinationSizeForPixelLimit(original, 800));
            assertEquals(new Size(0, 0), Reduce.destinationSizeForPixelLimit(original, 0));
            assertEquals(new Size(200, 100), Reduce.destinationSizeForPixelLimit(original, 20_000));

            // below limit
            assertEquals(new Size(100, 50), Reduce.destinationSizeForPixelLimit(original, 5001));
            assertEquals(new Size(100, 50), Reduce.destinationSizeForPixelLimit(original, 5040));
            assertEquals(new Size(40, 20), Reduce.destinationSizeForPixelLimit(original, 810));
            assertEquals(new Size(0, 0), Reduce.destinationSizeForPixelLimit(original, 1));
            assertEquals(new Size(200, 100), Reduce.destinationSizeForPixelLimit(original, 20_050));
        }
        {
            // 3:2 image might cause more rounding issues
            final Size original = new Size(600, 400);
            // hit limit
            assertEquals(new Size(300, 200), Reduce.destinationSizeForPixelLimit(original, 60_000));
            assertEquals(new Size(60, 40), Reduce.destinationSizeForPixelLimit(original, 2400));
            assertEquals(new Size(0, 0), Reduce.destinationSizeForPixelLimit(original, 0));
            assertEquals(new Size(900, 600), Reduce.destinationSizeForPixelLimit(original, 540_000));

            // below limit
            assertEquals(new Size(300, 200), Reduce.destinationSizeForPixelLimit(original, 60100));
            assertEquals(new Size(60, 40), Reduce.destinationSizeForPixelLimit(original, 2500));
            assertEquals(new Size(0, 0), Reduce.destinationSizeForPixelLimit(original, 1));
            assertEquals(new Size(900, 600), Reduce.destinationSizeForPixelLimit(original, 540_100));
        }
        {
            // Read world problematic image (ratio 4:3 or 1,333333333)
            final Size original = new Size(3264, 2448);

            // Image ratio is more important than maximal size.
            // 866x1155 exceeds limit (by 230)
            // 866x1154 changes ratio (ratio 1,33256351; 99,94%)
            // 865x1153 is good (ratio 1,332947977, 99,97%)
            assertEquals(new Size(1153, 865), Reduce.destinationSizeForPixelLimit(original, 1_000_000));
        }
    }

    @NonNull
    private String getResourcePath(@RawRes int id, final String extension) {
        try {
            File tmpDir = InstrumentationRegistry.getContext().getCacheDir();
            File tmpFile = File.createTempFile("prefix", "." + extension, tmpDir);

            // Copy resource to tmp
            InputStream in = InstrumentationRegistry.getContext().getResources().openRawResource(id);
            FileOutputStream out = new FileOutputStream(tmpFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.close();

            return tmpFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            throw new AssertionError("File must be available");
        }
    }

    @NonNull
    private String getOutPath(final String extension) {
        File tmpDir = InstrumentationRegistry.getContext().getCacheDir();
        File outFile;
        try {
            outFile = File.createTempFile("prefix", "." + extension, tmpDir);
        } catch (IOException e) {
            e.printStackTrace();
            throw new AssertionError("File must be available");
        }
        return outFile.getAbsolutePath();
    }

    @Test
    public void scale() {
        String inPath = getResourcePath(R.raw.couch, "jpg");
        String outPath = getOutPath("jpg");
        Reduce.scale(inPath, outPath, new Size(300, 200));

        Bitmap result = BitmapFactory.decodeFile(outPath);
        assertEquals(300, result.getWidth());
        assertEquals(200, result.getHeight());
    }

    @Test
    public void resize() {
        String inPath = getResourcePath(R.raw.couch, "jpg");
        String outPath = getOutPath("jpg");
        Reduce.resize(inPath, outPath, new Size(300, 200));

        Bitmap result = BitmapFactory.decodeFile(outPath);
        assertEquals(300, result.getWidth());
        assertEquals(200, result.getHeight());
    }
}
