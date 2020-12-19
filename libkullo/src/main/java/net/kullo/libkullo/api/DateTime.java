/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.libkullo.api;

import android.support.annotation.NonNull;

public class DateTime extends DateTimeBase implements Comparable<DateTime> {
    public DateTime(
            short year, byte month, byte day,
            byte hour, byte minute, byte second,
            short tzOffsetMinutes) {

        super(year, month, day, hour, minute, second, tzOffsetMinutes);

        // check validity
        if (!InternalDateTimeUtils.isValid(year, month, day, hour, minute, second, tzOffsetMinutes)) {
            throw new IllegalArgumentException("The arguments don't form a valid date/time");
        }
    }

    public static DateTime nowUtc() {
        return InternalDateTimeUtils.nowUtc();
    }

    @Override
    public String toString() {
        return InternalDateTimeUtils.toString(this);
    }

    @Override
    public int compareTo(@NonNull DateTime other) {
        return InternalDateTimeUtils.compare(this, other);
    }
}
