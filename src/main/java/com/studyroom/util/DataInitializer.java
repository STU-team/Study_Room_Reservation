package com.studyroom.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 按接口契约 v0.6 初始化七张 CSV 的表头，不覆盖已存在的数据文件。 */
public final class DataInitializer {
    private static final Map<String, List<String>> HEADERS = createHeaders();

    private DataInitializer() {
    }

    private static Map<String, List<String>> createHeaders() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("seats.csv", List.of("seatNo", "typeName", "status"));
        headers.put("seat_types.csv", List.of("typeName", "seatCount"));
        headers.put("reservations.csv", List.of("resNo", "studentName", "studentId", "phone",
                "seatNo", "resDate", "startTime", "endTime", "status"));
        headers.put("students.csv", List.of("name", "studentId", "phone", "creditScore"));
        headers.put("users.csv", List.of("username", "password", "role"));
        headers.put("checkin_records.csv", List.of("recordNo", "resNo", "actualStart", "actualEnd",
                "penalty", "createTime"));
        headers.put("credit_score_logs.csv", List.of("logId", "studentId", "reservationId",
                "changeValue", "reason", "createTime"));
        return Map.copyOf(headers);
    }

    /** 初始化目录下缺失的 CSV 文件；文件已有内容时只校验表头，不覆盖数据。 */
    public static void initialize(Path dataDirectory) throws IOException {
        if (dataDirectory == null) throw new IllegalArgumentException("数据目录不能为空");
        // 先校验所有已存在文件，避免遇到后续表头不匹配时留下半初始化目录。
        for (Map.Entry<String, List<String>> entry : HEADERS.entrySet()) {
            Path file = dataDirectory.resolve(entry.getKey());
            if (java.nio.file.Files.exists(file) && java.nio.file.Files.size(file) > 0L) {
                List<List<String>> rows = CsvUtil.readAll(file);
                if (rows.isEmpty() || !rows.get(0).equals(entry.getValue())) {
                    throw new IOException("CSV 表头与契约不一致，拒绝覆盖现有文件：" + file);
                }
            }
        }
        for (Map.Entry<String, List<String>> entry : HEADERS.entrySet()) {
            CsvUtil.ensureHeader(dataDirectory.resolve(entry.getKey()), entry.getValue());
        }
    }

    /** 返回契约约定的文件名及表头，供 DAO 与测试复用。 */
    public static Map<String, List<String>> headers() {
        return HEADERS;
    }
}
