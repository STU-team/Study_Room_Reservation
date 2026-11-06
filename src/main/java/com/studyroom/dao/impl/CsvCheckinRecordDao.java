package com.studyroom.dao.impl;

import com.studyroom.dao.CheckinRecordDao;
import com.studyroom.dao.DataAccessException;
import com.studyroom.entity.CheckinRecord;
import com.studyroom.util.CsvMapper;
import com.studyroom.util.CsvUtil;
import com.studyroom.util.DataInitializer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * CheckinRecordDao 的 CSV 实现。
 *
 * <p>契约 v0.6 的 checkin_records.csv 为
 * {@code recordNo,resNo,actualStart,actualEnd,penalty,createTime}，与实体一一对应，
 * 不需要额外映射。一条预约至多一条签到记录，记录写入后不再修改。</p>
 */
public class CsvCheckinRecordDao implements CheckinRecordDao {
    private static final List<String> HEADER = DataInitializer.headers().get("checkin_records.csv");
    private static final int COL_RECORD_NO = 0;
    private static final int COL_RES_NO = 1;
    private static final int COL_ACTUAL_START = 2;
    private static final int COL_ACTUAL_END = 3;
    private static final int COL_PENALTY = 4;
    private static final int COL_CREATE_TIME = 5;

    private final Path file;

    public CsvCheckinRecordDao(Path dataDirectory) {
        Objects.requireNonNull(dataDirectory, "dataDirectory 不能为空");
        this.file = dataDirectory.resolve("checkin_records.csv");
        try {
            CsvUtil.ensureHeader(file, HEADER);
        } catch (IOException ex) {
            throw new DataAccessException("初始化 checkin_records.csv 失败", ex);
        }
    }

    @Override
    public synchronized void save(CheckinRecord record) {
        Objects.requireNonNull(record, "record 不能为空");
        if (record.getRecordNo() == null || record.getRecordNo().isBlank()) {
            throw new IllegalArgumentException("recordNo 不能为空");
        }
        if (record.getResNo() == null || record.getResNo().isBlank()) {
            throw new IllegalArgumentException("resNo 不能为空");
        }
        if (record.getActualStart() == null) {
            throw new IllegalArgumentException("actualStart 不能为空");
        }
        List<List<String>> rows = readRows();
        for (int i = 1; i < rows.size(); i++) {
            String recordNo = rows.get(i).get(COL_RECORD_NO);
            if (record.getRecordNo().equals(recordNo)) {
                throw new DataAccessException("签到记录编号已存在：" + record.getRecordNo());
            }
            String resNo = rows.get(i).get(COL_RES_NO);
            if (record.getResNo().equals(resNo)) {
                throw new DataAccessException("该预约已存在签到记录：" + record.getResNo());
            }
        }
        rows.add(List.of(record.getRecordNo(), record.getResNo(),
                CsvMapper.formatDateTime(record.getActualStart()),
                CsvMapper.formatDateTime(record.getActualEnd()),
                CsvMapper.text(record.getPenalty()),
                CsvMapper.formatDateTime(record.getCreateTime() == null ? new Date() : record.getCreateTime())));
        writeRows(rows);
    }

    @Override
    public synchronized CheckinRecord findByResNo(String resNo) {
        if (resNo == null || resNo.isBlank()) {
            return null;
        }
        List<List<String>> rows = readRows();
        for (int i = 1; i < rows.size(); i++) {
            if (resNo.equals(rows.get(i).get(COL_RES_NO))) {
                return toRecord(rows.get(i));
            }
        }
        return null;
    }

    private CheckinRecord toRecord(List<String> row) {
        if (row.size() != HEADER.size()) {
            throw new DataAccessException("checkin_records.csv 行字段数量错误：" + row);
        }
        CheckinRecord record = new CheckinRecord(
                row.get(COL_RECORD_NO),
                CsvMapper.parseDateTime(row.get(COL_ACTUAL_START)),
                CsvMapper.parseDateTime(row.get(COL_ACTUAL_END)),
                CsvMapper.parseInteger(row.get(COL_PENALTY)));
        record.setResNo(row.get(COL_RES_NO));
        record.setCreateTime(CsvMapper.parseDateTime(row.get(COL_CREATE_TIME)));
        return record;
    }

    private List<List<String>> readRows() {
        try {
            List<List<String>> rows = CsvUtil.readAll(file);
            if (rows.isEmpty() || !HEADER.equals(rows.get(0))) {
                throw new DataAccessException("checkin_records.csv 表头缺失或与契约不一致");
            }
            return rows;
        } catch (IOException ex) {
            throw new DataAccessException("读取 checkin_records.csv 失败", ex);
        }
    }

    private void writeRows(List<List<String>> rows) {
        try {
            CsvUtil.writeAll(file, rows);
        } catch (IOException ex) {
            throw new DataAccessException("写入 checkin_records.csv 失败", ex);
        }
    }
}
