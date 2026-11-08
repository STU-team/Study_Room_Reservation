package com.studyroom.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

/** Service 用 java.time 表达日期时间，实体与 CSV 用 Date，在此统一转换（精确到分钟）。 */
public final class DateTimes {
    private DateTimes() {
    }

    public static Date at(LocalDate date, LocalTime time) {
        return Date.from(date.atTime(time.withSecond(0).withNano(0)).atZone(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDate date(Date value) {
        return value == null ? null : Instant.ofEpochMilli(value.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static LocalTime time(Date value) {
        return value == null ? null
                : Instant.ofEpochMilli(value.getTime()).atZone(ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0);
    }

    public static Date plusMinutes(Date value, int minutes) {
        return value == null ? null : new Date(value.getTime() + minutes * 60_000L);
    }
}
