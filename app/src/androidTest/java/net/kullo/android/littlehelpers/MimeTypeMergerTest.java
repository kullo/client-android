/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

import android.test.AndroidTestCase;

import java.util.Arrays;
import java.util.Collections;

public class MimeTypeMergerTest extends AndroidTestCase {
    public void testSingle() {
        assertEquals(MimeTypeMerger.merge(Collections.singletonList("image/png")), "image/png");
        assertEquals(MimeTypeMerger.merge(Collections.singletonList("application/vnd.openxmlformats-officedocument.presentationml.presentation")), "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        assertEquals(MimeTypeMerger.merge(Collections.singletonList("")), "");
    }

    public void testSameType() {
        assertEquals(MimeTypeMerger.merge(Arrays.asList(
                "image/png",
                "image/png"
        )), "image/png");

        assertEquals(MimeTypeMerger.merge(Arrays.asList(
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        )), "application/vnd.openxmlformats-officedocument.presentationml.presentation");
    }

    public void testSameTypeMultiple() {
        assertEquals(MimeTypeMerger.merge(Arrays.asList(
                "image/png",
                "image/png",
                "image/png",
                "image/png",
                "image/png",
                "image/png",
                "image/png",
                "image/png"
        )), "image/png");
    }

    public void testDifferentImages() {
        assertEquals(MimeTypeMerger.merge(Arrays.asList(
                "image/png",
                "image/jpeg"
        )), "image/*");

        assertEquals(MimeTypeMerger.merge(Arrays.asList(
                "image/png",
                "image/png",
                "image/jpeg"
        )), "image/*");

        assertEquals(MimeTypeMerger.merge(Arrays.asList(
                "image/bmp",
                "image/gif",
                "image/jpeg",
                "image/png",
                "image/tiff"
        )), "image/*");
    }

    public void testDifferentAudio() {
        assertEquals(MimeTypeMerger.merge(Arrays.asList(
                "audio/wav",
                "audio/ogg"
        )), "audio/*");

        assertEquals(MimeTypeMerger.merge(Arrays.asList(
                "audio/wav",
                "audio/wav",
                "audio/ogg"
        )), "audio/*");

        assertEquals(MimeTypeMerger.merge(Arrays.asList(
                "audio/wav",
                "audio/ogg",
                "audio/vorbis",
                "audio/mp3"
        )), "audio/*");
    }

    public void testDifferentTypes() {
        assertEquals(MimeTypeMerger.merge(Arrays.asList(
                "audio/wav",
                "image/png"
        )), "*/*");

        assertEquals(MimeTypeMerger.merge(Arrays.asList(
                "audio/wav",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        )), "*/*");

        assertEquals(MimeTypeMerger.merge(Arrays.asList(
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "image/png"
        )), "*/*");
    }
}
