package com.studyroom.dao.impl;

import com.studyroom.entity.CreditChangeReason;
import com.studyroom.entity.CreditScoreLog;
import com.studyroom.util.CsvMapper;
import com.studyroom.util.CsvStore;
import com.studyroom.util.DataInitializer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** 信用分流水读写（契约第七节未声明接口，暂以独立实现类提供）。 */
public class CsvCreditScoreLogDao {
    private static final List<String> HEADER = DataInitializer.headers().get("credit_score_logs.csv");
    private final CsvStore store;

    public CsvCreditScoreLogDao(Path dataDirectory) {
        this.store = new CsvStore(dataDirectory.resolve("credit_score_logs.csv"), HEADER);
    }

    public synchronized void save(CreditScoreLog log) {
        List<List<String>> rows = store.all();
        long next = rows.size();  // 行数即下一个自增号，表头占 1 行
        for (int i = 1; i < rows.size(); i++) next = Math.max(next, toLong(rows.get(i).get(0)) + 1);
        if (log.getLogId() == null) log.setLogId(next);
        rows.add(List.of(String.valueOf(log.getLogId()), log.getStudentId(),
                CsvMapper.text(log.getReservationId()), String.valueOf(log.getChangeValue()),
                log.getReason().name(),
                CsvMapper.time(log.getCreateTime() == null ? new Date() : log.getCreateTime())));
        store.save(rows);
    }

    public synchronized List<CreditScoreLog> findByStudentId(String studentId) {
        List<CreditScoreLog> list = new ArrayList<>();
        for (List<String> row : store.data()) {
            if (studentId != null && studentId.equals(row.get(1))) list.add(toLog(row));
        }
        return list;
    }

    public synchronized List<CreditScoreLog> findAll() {
        return store.data().stream().map(this::toLog).toList();
    }

    private CreditScoreLog toLog(List<String> row) {
        return new CreditScoreLog(
                row.get(0).isBlank() ? null : toLong(row.get(0)),
                row.get(1), row.get(2).isBlank() ? null : toLong(row.get(2)),
                CsvMapper.integer(row.get(3)), CreditChangeReason.valueOf(row.get(4)),
                CsvMapper.time(row.get(5)));
    }

    private long toLong(String text) {
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
