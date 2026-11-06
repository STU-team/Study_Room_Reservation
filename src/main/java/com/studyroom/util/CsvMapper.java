package com.studyroom.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * CSV 字段与实体字段之间的通用转换：日期时间格式、业务编号到主键的稳定映射。
 *
 * <p>契约 v0.6 第八节的部分 CSV 表只保存业务编号（seatNo、resNo、username、studentId），
 * 而 A-3 实体另有 Long 主键。为了在不修改契约的前提下做到「重启后仍能还原同一实体」，
 * 这里用业务编号的稳定散列作为主键：同一编号在任何一次运行中都得到同一个 Long，
 * 不依赖行序，也不引入额外列。</p>
 */
public final class CsvMapper {
    /** 契约第八节约定的日期时间格式。 */
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm";

    private CsvMapper() {
    }

    /** 按契约格式输出日期时间；null 输出空串。 */
    public static String formatDateTime(Date value) {
        return value == null ? "" : new SimpleDateFormat(DATE_TIME_PATTERN, Locale.ROOT).format(value);
    }

    /** 解析契约格式的日期时间；空串返回 null。兼容只写日期 `yyyy-MM-dd` 的旧数据。 */
    public static Date parseDateTime(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim();
        try {
            if (value.length() <= 10) {
                return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(value);
            }
            return new SimpleDateFormat(DATE_TIME_PATTERN, Locale.ROOT).parse(value);
        } catch (ParseException ex) {
            throw new IllegalArgumentException("CSV 日期时间格式应为 " + DATE_TIME_PATTERN + "：" + value, ex);
        }
    }

    /**
     * 由业务编号得到稳定的 Long 主键（FNV-1a 64 位，取正）。
     * 同一编号恒等，便于把 seatNo / resNo / username 映射为实体里的 Long 主键。
     */
    public static Long stableId(String businessKey) {
        if (businessKey == null || businessKey.isBlank()) {
            throw new IllegalArgumentException("业务编号不能为空");
        }
        long hash = 0xcbf29ce484222325L;
        for (byte b : businessKey.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            hash ^= (b & 0xff);
            hash *= 0x100000001b3L;
        }
        return hash & 0x7FFFFFFFFFFFFFFFL;
    }

    /** 输出到 CSV 的字符串；null 输出空串。 */
    public static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /** 解析整数；空串返回 null。 */
    public static Integer parseInteger(String text) {
        return text == null || text.isBlank() ? null : Integer.valueOf(text.trim());
    }
}
