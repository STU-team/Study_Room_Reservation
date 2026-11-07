package com.studyroom.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 通用 CSV 工具：UTF-8、逗号/双引号/换行转义、原子写入。
 * 不依赖具体业务实体，供各 CSV DAO 复用。
 */
public final class CsvUtil {
    private CsvUtil() {
    }

    /** 读取 CSV 全部记录；每条记录对应一行，每个字段均为字符串。 */
    public static List<List<String>> readAll(Path file) throws IOException {
        Objects.requireNonNull(file, "file 不能为空");
        if (!Files.exists(file)) {
            return List.of();
        }
        String content = Files.readString(file, StandardCharsets.UTF_8);
        return parse(content);
    }

    /** 将记录写入 CSV。目标文件先写入同目录临时文件，再替换，避免写入中断破坏原文件。 */
    public static void writeAll(Path file, List<? extends List<String>> rows) throws IOException {
        Objects.requireNonNull(file, "file 不能为空");
        Objects.requireNonNull(rows, "rows 不能为空");
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        StringBuilder content = new StringBuilder();
        for (List<String> row : rows) {
            if (row == null) {
                throw new IllegalArgumentException("CSV 记录不能为 null");
            }
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) content.append(',');
                content.append(escape(row.get(i)));
            }
            content.append('\n');
        }

        String prefix = file.getFileName().toString();
        if (prefix.length() < 3) prefix = (prefix + "___").substring(0, 3);
        Path temp = Files.createTempFile(parent, prefix, ".tmp");
        try {
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    /** 追加一条记录；文件不存在时创建。 */
    public static void appendRow(Path file, List<String> row) throws IOException {
        Objects.requireNonNull(file, "file 不能为空");
        Objects.requireNonNull(row, "row 不能为空");
        List<List<String>> rows = new ArrayList<>(readAll(file));
        rows.add(new ArrayList<>(row));
        writeAll(file, rows);
    }

    /** 确保文件存在且首行为指定表头；已有文件不会被覆盖。 */
    public static void ensureHeader(Path file, List<String> header) throws IOException {
        Objects.requireNonNull(file, "file 不能为空");
        Objects.requireNonNull(header, "header 不能为空");
        if (header.isEmpty()) {
            throw new IllegalArgumentException("CSV 表头不能为空");
        }
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);

        if (!Files.exists(file) || Files.size(file) == 0L) {
            writeAll(file, List.of(header));
            return;
        }

        List<List<String>> rows = readAll(file);
        if (rows.isEmpty() || !rows.get(0).equals(header)) {
            throw new IOException("CSV 表头与契约不一致，拒绝覆盖现有文件：" + file);
        }
    }

    /** 将一个字段按 CSV 规则转义。 */
    public static String escape(String value) {
        if (value == null) return "";
        boolean quote = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0
                || (!value.isEmpty() && (Character.isWhitespace(value.charAt(0))
                || Character.isWhitespace(value.charAt(value.length() - 1))));
        if (!quote) return value;
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    /** 解析 CSV 文本，支持双引号字段、字段内逗号、双引号和换行。 */
    static List<List<String>> parse(String content) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        if (content == null || content.isEmpty()) return rows;

        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean afterQuote = false;
        boolean fieldStarted = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (inQuotes) {
                if (c == '\"') {
                    if (i + 1 < content.length() && content.charAt(i + 1) == '\"') {
                        field.append('\"');
                        i++;
                    } else {
                        inQuotes = false;
                        afterQuote = true;
                    }
                } else {
                    field.append(c);
                }
                continue;
            }

            if (afterQuote) {
                if (c == ',') {
                    row.add(field.toString());
                    field.setLength(0);
                    fieldStarted = false;
                    afterQuote = false;
                } else if (c == '\n' || c == '\r') {
                    if (c == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') i++;
                    row.add(field.toString());
                    rows.add(row);
                    row = new ArrayList<>();
                    field.setLength(0);
                    fieldStarted = false;
                    afterQuote = false;
                } else {
                    throw new IOException("CSV 格式错误：带引号字段结束后出现非法字符");
                }
            } else if (c == '\"') {
                if (fieldStarted || field.length() > 0) {
                    throw new IOException("CSV 格式错误：引号出现在未加引号字段的中间");
                }
                inQuotes = true;
                fieldStarted = true;
            } else if (c == ',') {
                row.add(field.toString());
                field.setLength(0);
                fieldStarted = false;
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') i++;
                row.add(field.toString());
                rows.add(row);
                row = new ArrayList<>();
                field.setLength(0);
                fieldStarted = false;
            } else {
                field.append(c);
                fieldStarted = true;
            }
        }

        if (inQuotes) throw new IOException("CSV 格式错误：双引号字段未闭合");
        if (field.length() > 0 || fieldStarted || !row.isEmpty() || afterQuote) {
            row.add(field.toString());
            rows.add(row);
        }

        // 去除 UTF-8 BOM（如果文件由某些 Windows 工具保存）。
        if (!rows.isEmpty() && !rows.get(0).isEmpty() && rows.get(0).get(0).startsWith("\uFEFF")) {
            rows.get(0).set(0, rows.get(0).get(0).substring(1));
        }
        return rows;
    }
}
