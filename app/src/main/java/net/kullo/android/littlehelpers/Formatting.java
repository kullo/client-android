/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.littlehelpers;

import android.content.Context;
import android.support.annotation.NonNull;

import net.kullo.android.R;
import net.kullo.android.application.KulloApplication;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormatter;

import java.util.Locale;
import java.util.regex.Pattern;

public class Formatting {
    // Use binary multiplier 1024 here to avoid having a "102 MB" file in Kullo
    // indicating that files greater that 100 megabytes can be sent.
    // Use unit titles "MB" and "KB" known from Windows Explorer.
    @SuppressWarnings("FieldCanBeLocal") private static final long GIGA = 1024*1024*1024;
    @SuppressWarnings("FieldCanBeLocal") private static final long MEGA = 1024*1024;
    @SuppressWarnings("FieldCanBeLocal") private static final long KILO = 1024;

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private static final DateTimeZone LOCAL_TIME_ZONE = DateTimeZone.getDefault();
    private static final DateTimeFormatter FORMATTER_CALENDAR_DATE = KulloApplication.sharedInstance.getShortDateFormatter();
    private static final DateTimeFormatter FORMATTER_CLOCK = KulloApplication.sharedInstance.getShortTimeFormatter();

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
        LocalDateTime localDateReceived = new LocalDateTime(dateLatestMessage, LOCAL_TIME_ZONE);

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

    public static String compressedText(@NonNull final String text) {
        return WHITESPACE_PATTERN.matcher(text).replaceAll(" ");
    }

    public static String shortDateText(DateTime dateReceived) {
        LocalDateTime localDateReceived = new LocalDateTime(dateReceived, LOCAL_TIME_ZONE);

        String dateString;
        if(localDateReceived.toLocalDate().equals(new LocalDate())) {
            dateString = localDateReceived.toString(FORMATTER_CLOCK);
        } else if(localDateReceived.toLocalDate().equals((new LocalDate()).minusDays(1))) {
            dateString = KulloApplication.sharedInstance.getString(R.string.yesterday);
        } else {
            dateString = localDateReceived.toString(FORMATTER_CALENDAR_DATE);
        }

        return dateString;
    }
}
