/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

public class Formatting {
    public static String filesizeHuman(long bytes) {
        if (bytes > 8000000)
            return String.format("%.0f MB", bytes / 1000000.0);
        else if (bytes > 1000000)
            return String.format("%.1f MB", bytes / 1000000.0);
        else if (bytes > 8000)
            return String.format("%.0f kB", bytes / 1000.0);
        else if (bytes > 1000)
            return String.format("%.1f kB", bytes / 1000.0);
        else
            return String.format("%d  Bytes", bytes);
    }

    private Formatting() {}
}
