/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class Debug {
    public static String getUriDetails(Uri uri) {
        if (uri == null) return "null";

        return String.format("Uri: %s\n" +
                        "  scheme: %s\n" +
                        "  path: %s\n" +
                        "  scheme specific part: %s\n" +
                        "  encoded scheme specific part: %s\n" +
                        "  fragment: %s\n" +
                        "  fragment encoded: %s",
                uri.toString(),
                uri.getScheme(),
                uri.getPath(),
                uri.getSchemeSpecificPart(),
                uri.getEncodedSchemeSpecificPart(),
                uri.getFragment(),
                uri.getEncodedFragment()
        );
    }

    public static String getIntentDetails(Intent intent) {
        Uri data = intent.getData();
        Bundle extras = intent.getExtras();
        return String.format("Intent: %s\n" +
                "  Data: %s\n" +
                "    scheme: %s\n" +
                "    scheme specific part: %s\n" +
                "    encoded scheme specific part: %s\n" +
                "    fragment: %s\n" +
                "    fragment encoded: %s\n" +
                "  Extras: %s",
                intent,
                data != null ? data.toString() : "null",
                data != null ? data.getScheme() : "",
                data != null ? data.getSchemeSpecificPart() : "",
                data != null ? data.getEncodedSchemeSpecificPart() : "",
                data != null ? data.getFragment() : "",
                data != null ? data.getEncodedFragment() : "",
                extras != null ? extras.toString() : "null"
        );
    }
}
