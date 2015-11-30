/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

import android.content.Intent;
import android.net.Uri;

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
        return String.format("Intent: %s\n" +
                "  Data: %s\n" +
                "    scheme: %s\n" +
                "    scheme specific part: %s\n" +
                "    encoded scheme specific part: %s\n" +
                "    fragment: %s\n" +
                "    fragment encoded: %s\n" +
                "  Extras: %s",
                intent,
                intent.getData().toString(),
                intent.getData().getScheme(),
                intent.getData().getSchemeSpecificPart(),
                intent.getData().getEncodedSchemeSpecificPart(),
                intent.getData().getFragment(),
                intent.getData().getEncodedFragment(),
                (intent.getExtras() != null ? intent.getExtras().toString() : "null")
        );
    }
}
