/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.webkit.MimeTypeMap;

import java.util.Collection;
import java.util.Objects;

public class UriHelpers {
    public static boolean isFileUri(@NonNull final Uri uri) {
        return uri.getScheme().equals("file");
    }

    public static boolean containsFileUri(@NonNull final Collection<Uri> uris) {
        boolean out = false;
        for (final Uri uri : uris) {
            out |= isFileUri(uri);
        }
        return out;
    }

    @NonNull
    public static String getFilename(@NonNull final Context context,
                                     @NonNull final Uri uri,
                                     @NonNull final String fallbackFilename) {
        String out = null;

        switch (uri.getScheme()) {
            case "content": {
                // Get filename from stream
                String displayName = null;
                Cursor fileInfoCursor = context.getContentResolver().query(uri, null, null, null, null);
                if (fileInfoCursor != null) {
                    fileInfoCursor.moveToFirst();
                    int nameIndex = fileInfoCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        displayName = fileInfoCursor.getString(nameIndex);
                    }
                    fileInfoCursor.close();
                }

                if (displayName == null || displayName.isEmpty()) {
                    displayName = "asset";
                }

                if (!displayNameContainsExtension(displayName)) {
                    String mimeType = context.getContentResolver().getType(uri);
                    MimeTypeMap mime = MimeTypeMap.getSingleton();
                    String ext = mime.getExtensionFromMimeType(mimeType);
                    if (ext != null && !ext.isEmpty()) {
                        displayName += "." + ext;
                    }
                }

                out = displayName;
            } break;
            case "file": {
                out = uri.getLastPathSegment();
            } break;

        }

        if (out == null) out = fallbackFilename;

        return out;
    }

    public static boolean displayNameContainsExtension(@NonNull String displayName) {
        return displayName.matches(".*\\.[a-zA-Z0-9]+");
    }
}
