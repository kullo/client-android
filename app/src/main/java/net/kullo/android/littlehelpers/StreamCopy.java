/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.util.Log;

import net.kullo.javautils.RuntimeAssertion;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamCopy {
    private static final String TAG = "StreamCopy";

    @CheckResult
    public static boolean copyToPath(Context context, final Uri selectedFileUri, final String destPath) {
        {
            // Supported uri schemes: content://, file://, android.resource://
            // https://developer.android.com/reference/android/content/ContentResolver.html#openInputStream(android.net.Uri)
            final String scheme = selectedFileUri.getScheme();
            RuntimeAssertion.require(scheme.equals(ContentResolver.SCHEME_CONTENT)
                    || scheme.equals(ContentResolver.SCHEME_FILE)
                    || scheme.equals(ContentResolver.SCHEME_ANDROID_RESOURCE));
        }

        InputStream inputStream;
        OutputStream outputStream;

        try {
            inputStream = context.getContentResolver().openInputStream(selectedFileUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        RuntimeAssertion.require(inputStream != null);

        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(destPath, false));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        return copyAndCloseStreams(inputStream, outputStream);
    }

    @CheckResult
    public static boolean copyToUri(Context context, final Uri source, final Uri target) {
        {
            // Supported uri schemes: content://, file://, android.resource://
            // https://developer.android.com/reference/android/content/ContentResolver.html#openInputStream(android.net.Uri)
            final String scheme = source.getScheme();
            RuntimeAssertion.require(scheme.equals(ContentResolver.SCHEME_CONTENT)
                    || scheme.equals(ContentResolver.SCHEME_FILE)
                    || scheme.equals(ContentResolver.SCHEME_ANDROID_RESOURCE));
        }

        {
            // Supported uri schemes: content://, file://
            // https://developer.android.com/reference/android/content/ContentResolver.html#openOutputStream(android.net.Uri,%20java.lang.String)
            final String scheme = target.getScheme();
            RuntimeAssertion.require(scheme.equals(ContentResolver.SCHEME_CONTENT)
                    || scheme.equals(ContentResolver.SCHEME_FILE));
        }

        InputStream inputStream;
        OutputStream outputStream;

        try {
            inputStream = context.getContentResolver().openInputStream(source);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        RuntimeAssertion.require(inputStream != null);

        try {
            outputStream = context.getContentResolver().openOutputStream(target);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        RuntimeAssertion.require(outputStream != null);

        return copyAndCloseStreams(inputStream, outputStream);
    }

    @CheckResult
    private static boolean copyAndCloseStreams(@NonNull InputStream inputStream, @NonNull OutputStream outputStream) {
        boolean ok = true;

        try {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
            ok = false;
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing input stream");
                e.printStackTrace();
            }

            try {
                outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing output stream");
                e.printStackTrace();
            }
        }

        return ok;
    }
}
