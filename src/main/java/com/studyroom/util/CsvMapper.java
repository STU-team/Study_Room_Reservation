package com.studyroom.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** CSV 与实体之间的字段转换：时间格式、业务编号到 Long 主键的稳定映射。 */
public final class CsvMapper {
    private static final String PATTERN = "yyyy-MM-dd HH:mm";

    private CsvMapper() {
    }

    public static String time(Date value) {
        return value == null ? "" : new SimpleDateFormat(PATTERN, Locale.ROOT).format(value);
    }

    public static Date time(String text) {
        if (text == null || text.isBlank()) return null;
        String value = text.trim();
        try {
            return new SimpleDateFormat(value.length() <= 10 ? "yyyy-MM-dd" : PATTERN, Locale.ROOT).parse(value);
        } catch (ParseException e) {
            throw new IllegalArgumentException("日期时间格式应为 " + PATTERN + "：" + value, e);
        }
    }

    /** 由业务编号得到稳定 Long 主键（FNV-1a 63 位），同一编号重启后仍相同。 */
    public static Long id(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("业务编号不能为空");
        long hash = 0xcbf29ce484222325L;
        for (byte b : key.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            hash = (hash ^ (b & 0xff)) * 0x100000001b3L;
        }
        return hash & 0x7FFFFFFFFFFFFFFFL;
    }

    public static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public static Integer integer(String text) {
        return text == null || text.isBlank() ? null : Integer.valueOf(text.trim());
    }
}
