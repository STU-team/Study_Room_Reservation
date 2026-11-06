package com.studyroom.util;

import com.studyroom.dao.DataAccessException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** 一张 CSV 表的读写封装：表头锁定、行宽统一校验、统一异常。 */
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

    /** 含表头的整表；任何一行字段数与表头不符都立即报错。 */
    public List<List<String>> all() {
        try {
            List<List<String>> rows = CsvUtil.readAll(file);
            if (rows.isEmpty() || !header.equals(rows.get(0))) {
                throw new DataAccessException("表头与契约不一致：" + file.getFileName());
            }
            for (int i = 1; i < rows.size(); i++) {
                if (rows.get(i).size() != header.size()) {
                    throw new DataAccessException(file.getFileName() + " 第 " + (i + 1) + " 行字段数与表头不一致");
                }
            }
            return rows;
        } catch (IOException e) {
            throw new DataAccessException("读取失败：" + file.getFileName(), e);
        }
    }

    /** 不含表头的数据行。 */
    public List<List<String>> data() {
        List<List<String>> rows = all();
        return rows.subList(1, rows.size());
    }

    public void save(List<List<String>> rows) {
        try {
            CsvUtil.writeAll(file, rows);
        } catch (IOException e) {
            throw new DataAccessException("写入失败：" + file.getFileName(), e);
        }
    }

    /** 按某列取值找行号（含表头下标），找不到返回 -1。 */
    public int indexOf(List<List<String>> rows, int column, String value) {
        if (value == null) return -1;
        for (int i = 1; i < rows.size(); i++) {
            if (value.equals(rows.get(i).get(column))) return i;
        }
        return -1;
    }
}
