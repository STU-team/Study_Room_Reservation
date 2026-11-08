package com.studyroom.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 按接口契约 v1.2 初始化七张 CSV 的表头，不覆盖已存在的数据文件。 */
public final class DataInitializer {
    private static final Map<String, List<String>> HEADERS = createHeaders();

    private DataInitializer() {
    }

    private static Map<String, List<String>> createHeaders() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("seats.csv", List.of("seatNo", "typeName", "status"));
        headers.put("seat_types.csv", List.of("typeName", "seatCount"));
        headers.put("reservations.csv", List.of("resNo", "studentName", "studentId", "phone",
                "seatNo", "resDate", "startTime", "endTime", "status", "createTime"));
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

    /** 首次启动写入示例数据（契约第八节）；某张表已有数据行时跳过该表，不覆盖用户数据。 */
    public static void seedSamples(Path dataDirectory) throws IOException {
        if (dataDirectory == null) throw new IllegalArgumentException("数据目录不能为空");
        seed(dataDirectory, "seats.csv", List.of(
                List.of("A01", "靠窗区", "AVAILABLE"),
                List.of("A02", "靠窗区", "AVAILABLE"),
                List.of("B01", "沙发区", "AVAILABLE"),
                List.of("B02", "沙发区", "AVAILABLE")));
        seed(dataDirectory, "seat_types.csv", List.of(
                List.of("靠窗区", "2"),
                List.of("沙发区", "2")));
        seed(dataDirectory, "students.csv", List.of(
                List.of("张三", "20240001", "13800000000", "100"),
                List.of("李四", "20240002", "13900000000", "100")));
        seed(dataDirectory, "users.csv", List.of(
                List.of("admin", "123456", "SYS_ADMIN"),
                List.of("duty", "123456", "DUTY_ADMIN"),
                List.of("20240001", "123456", "STUDENT"),
                List.of("20240002", "123456", "STUDENT")));
    }

    private static void seed(Path dataDirectory, String file, List<List<String>> samples) throws IOException {
        Path path = dataDirectory.resolve(file);
        CsvUtil.ensureHeader(path, HEADERS.get(file));
        if (CsvUtil.readAll(path).size() <= 1) CsvUtil.writeAll(path, withHeader(file, samples));
    }

    private static List<List<String>> withHeader(String file, List<List<String>> samples) {
        List<List<String>> rows = new java.util.ArrayList<>();
        rows.add(HEADERS.get(file));
        rows.addAll(samples);
        return rows;
    }
}
