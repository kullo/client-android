/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.littlehelpers;

import android.content.Context;

import net.kullo.android.R;
import net.kullo.android.application.KulloApplication;
import net.kullo.android.screens.conversationslist.ConversationsAdapter;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormatter;

import java.util.Locale;

public class Formatting {
    // Use binary multiplier 1024 here to avoid having a "102 MB" file in Kullo
    // indicating that files greater that 100 megabytes can be sent.
    // Use unit titles "MB" and "KB" known from Windows Explorer.
    @SuppressWarnings("FieldCanBeLocal") private static final long GIGA = 1024*1024*1024;
    @SuppressWarnings("FieldCanBeLocal") private static final long MEGA = 1024*1024;
    @SuppressWarnings("FieldCanBeLocal") private static final long KILO = 1024;

    public static String filesizeHuman(long bytes) {
        Locale locale = Locale.getDefault();

        if (bytes >= 8*GIGA)
            return String.format(locale, "%.0f GB", (double) bytes / GIGA);
        else if (bytes >= GIGA)
            return String.format(locale, "%.1f GB", (double) bytes / GIGA);
        else if (bytes >= 80*MEGA)
            return String.format(locale, "%.0f MB", (double) bytes / MEGA);
        else if (bytes >= MEGA)
            return String.format(locale, "%.1f MB", (double) bytes / MEGA);
        else if (bytes >= 8*KILO)
            return String.format(locale, "%.0f KB", (double) bytes / KILO);
        else if (bytes >= KILO)
            return String.format(locale, "%.1f KB", (double) bytes / KILO);
        else
            return String.format(locale, "%d Bytes", bytes);
    }

    public static String quotaInGib(long bytesUsed, long bytesQuota) {
        Locale locale = Locale.getDefault();
        return String.format(locale, "%.2f/%.1f GB",
            (double) bytesUsed / GIGA,
            (double) bytesQuota / GIGA
        );
    }

    private Formatting() {}

    public static String getLocalDateText(DateTime dateLatestMessage) {
        Context context = KulloApplication.sharedInstance;
        DateTimeFormatter formatterCalendarDate = KulloApplication.sharedInstance.getShortDateFormatter();
        LocalDateTime localDateReceived = new LocalDateTime(dateLatestMessage, ConversationsAdapter.LOCAL_TIME_ZONE);

        String dateString;
        if(localDateReceived.toLocalDate().equals(new LocalDate())) {
            dateString = context.getResources().getString(R.string.today);
        } else if(localDateReceived.toLocalDate().equals((new LocalDate()).minusDays(1))) {
            dateString = context.getResources().getString(R.string.yesterday);
        } else {
            dateString = localDateReceived.toString(formatterCalendarDate);
        }

        return dateString;
    }

    public static int perMilleRounded(long processedCount, long totalCount) {
        if (totalCount <= 0) return 0;
        return Math.round(1000 * ((float) processedCount / totalCount));
    }
}
