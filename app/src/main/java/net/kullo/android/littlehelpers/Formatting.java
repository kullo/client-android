/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

import java.util.Locale;

public class Formatting {
    public static String filesizeHuman(long bytes) {
        // Use binary multiplier 1024 here to avoid having a "102 MB" file in Kullo
        // indicating that files greater that 100 megabytes can be sent.
        // Use unit titles "MB" and "KB" known from Windows Explorer.
        final int MEGA = 1024*1024;
        final int KILO = 1024;

        Locale locale = Locale.getDefault();

        if (bytes >= 80*MEGA)
            return String.format(locale, "%.0f MB", (float) bytes / MEGA);
        else if (bytes >= MEGA)
            return String.format(locale, "%.1f MB", (float) bytes / MEGA);
        else if (bytes >= 8*KILO)
            return String.format(locale, "%.0f KB", (float) bytes / KILO);
        else if (bytes >= KILO)
            return String.format(locale, "%.1f KB", (float) bytes / KILO);
        else
            return String.format(locale, "%d Bytes", bytes);
    }

    private Formatting() {}
}
