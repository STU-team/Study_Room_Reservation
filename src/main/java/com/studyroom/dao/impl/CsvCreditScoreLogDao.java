package com.studyroom.dao.impl;

import com.studyroom.dao.DataAccessException;
import com.studyroom.entity.CreditChangeReason;
import com.studyroom.entity.CreditScoreLog;
import com.studyroom.util.CsvMapper;
import com.studyroom.util.CsvUtil;
import com.studyroom.util.DataInitializer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 信用分流水的 CSV 实现（契约 v0.6 第八节 credit_score_logs.csv）。
 *
 * <p>契约第七节尚未声明 CreditScoreLogDao 接口，为避免擅自扩展已定稿契约，本类暂以独立实现类提供
 * 读写能力，签名与「save / findByStudentId / findAll」三项一致，待契约补充接口后再实现该接口。</p>
 */
public class CsvCreditScoreLogDao {
    private static final List<String> HEADER = DataInitializer.headers().get("credit_score_logs.csv");
    private static final int COL_LOG_ID = 0;
    private static final int COL_STUDENT_ID = 1;
    private static final int COL_RESERVATION_ID = 2;
    private static final int COL_CHANGE_VALUE = 3;
    private static final int COL_REASON = 4;
    private static final int COL_CREATE_TIME = 5;

    private final Path file;

    public CsvCreditScoreLogDao(Path dataDirectory) {
        Objects.requireNonNull(dataDirectory, "dataDirectory 不能为空");
        this.file = dataDirectory.resolve("credit_score_logs.csv");
        try {
            CsvUtil.ensureHeader(file, HEADER);
        } catch (IOException ex) {
            throw new DataAccessException("初始化 credit_score_logs.csv 失败", ex);
        }
    }

    public synchronized void save(CreditScoreLog log) {
        Objects.requireNonNull(log, "log 不能为空");
        if (log.getStudentId() == null || log.getStudentId().isBlank()) {
            throw new IllegalArgumentException("studentId 不能为空");
        }
        if (log.getChangeValue() == null || log.getReason() == null) {
            throw new IllegalArgumentException("changeValue 与 reason 不能为空");
        }
        List<List<String>> rows = readRows();
        long nextId = 1;
        for (int i = 1; i < rows.size(); i++) {
            String id = rows.get(i).get(COL_LOG_ID);
            try {
                nextId = Math.max(nextId, Long.parseLong(id.trim()) + 1);
            } catch (NumberFormatException ignored) {
                // 历史脏数据不参与自增计算
            }
        }
        if (log.getLogId() == null) {
            log.setLogId(nextId);
        }
        Date createTime = log.getCreateTime() == null ? new Date() : log.getCreateTime();
        rows.add(List.of(String.valueOf(log.getLogId()), log.getStudentId(),
                CsvMapper.text(log.getReservationId()),
                String.valueOf(log.getChangeValue()),
                log.getReason().name(),
                CsvMapper.formatDateTime(createTime)));
        writeRows(rows);
    }

    public synchronized List<CreditScoreLog> findByStudentId(String studentId) {
        List<CreditScoreLog> logs = new ArrayList<>();
        if (studentId == null || studentId.isBlank()) {
            return logs;
        }
        for (CreditScoreLog log : findAll()) {
            if (studentId.equals(log.getStudentId())) {
                logs.add(log);
            }
        }
        return logs;
    }

    public synchronized List<CreditScoreLog> findAll() {
        List<List<String>> rows = readRows();
        List<CreditScoreLog> logs = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            logs.add(toLog(rows.get(i)));
        }
        return logs;
    }

    private CreditScoreLog toLog(List<String> row) {
        if (row.size() != HEADER.size()) {
            throw new DataAccessException("credit_score_logs.csv 行字段数量错误：" + row);
        }
        CreditChangeReason reason;
        try {
            reason = CreditChangeReason.valueOf(row.get(COL_REASON).trim());
        } catch (IllegalArgumentException ex) {
            throw new DataAccessException("credit_score_logs.csv 变动原因非法：" + row.get(COL_REASON), ex);
        }
        Long logId = row.get(COL_LOG_ID).isBlank() ? null : Long.valueOf(row.get(COL_LOG_ID).trim());
        Long reservationId = row.get(COL_RESERVATION_ID).isBlank()
                ? null : Long.valueOf(row.get(COL_RESERVATION_ID).trim());
        return new CreditScoreLog(logId, row.get(COL_STUDENT_ID), reservationId,
                CsvMapper.parseInteger(row.get(COL_CHANGE_VALUE)), reason,
                CsvMapper.parseDateTime(row.get(COL_CREATE_TIME)));
    }

    private List<List<String>> readRows() {
        try {
            List<List<String>> rows = CsvUtil.readAll(file);
            if (rows.isEmpty() || !HEADER.equals(rows.get(0))) {
                throw new DataAccessException("credit_score_logs.csv 表头缺失或与契约不一致");
            }
            return rows;
        } catch (IOException ex) {
            throw new DataAccessException("读取 credit_score_logs.csv 失败", ex);
        }
    }

    private void writeRows(List<List<String>> rows) {
        try {
            CsvUtil.writeAll(file, rows);
        } catch (IOException ex) {
            throw new DataAccessException("写入 credit_score_logs.csv 失败", ex);
        }
    }
}
