package com.studyroom.util;

import com.studyroom.dao.DataAccessException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** 一张 CSV 表的读写封装：表头锁定、每次操作校验、统一异常。 */
public final class CsvStore {
    private final Path file;
    private final List<String> header;

    public CsvStore(Path file, List<String> header) {
        this.file = file;
        this.header = header;
        try {
            CsvUtil.ensureHeader(file, header);
        } catch (IOException e) {
            throw new DataAccessException("初始化失败：" + file.getFileName(), e);
        }
    }

    /** 含表头的整表。 */
    public List<List<String>> all() {
        try {
            List<List<String>> rows = CsvUtil.readAll(file);
            if (rows.isEmpty() || !header.equals(rows.get(0))) {
                throw new DataAccessException("表头与契约不一致：" + file.getFileName());
            }
            return rows;
        } catch (IOException e) {
            throw new DataAccessException("读取失败：" + file.getFileName(), e);
        }
    }

    /** 不含表头的数据行。 */
    public List<List<String>> data() {
        List<List<String>> rows = all();
        return new ArrayList<>(rows.subList(1, rows.size()));
    }

    public void save(List<List<String>> rows) {
        try {
            CsvUtil.writeAll(file, rows);
        } catch (IOException e) {
            throw new DataAccessException("写入失败：" + file.getFileName(), e);
        }
    }

    /** 在数据行中按某列取值找行号（0 起，对应不含表头的列表）；找不到返回 -1。 */
    public int indexOf(List<List<String>> rows, int column, String value) {
        for (int i = 1; i < rows.size(); i++) {
            if (value != null && value.equals(rows.get(i).get(column))) {
                return i;
            }
        }
        return -1;
    }

    /** 行字段数必须与表头一致。 */
    public void requireWidth(List<String> row) {
        if (row.size() != header.size()) {
            throw new DataAccessException("字段数与表头不一致：" + row);
        }
    }
}
