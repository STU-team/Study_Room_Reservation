package com.studyroom.dao.impl;

import com.studyroom.dao.CheckinRecordDao;
import com.studyroom.dao.DataAccessException;
import com.studyroom.entity.CheckinRecord;
import com.studyroom.util.CsvMapper;
import com.studyroom.util.CsvStore;
import com.studyroom.util.DataInitializer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** CheckinRecordDao 的 CSV 实现：表头与实体一一对应，一条预约只允许一条记录。 */
public class CsvCheckinRecordDao implements CheckinRecordDao {
    private static final List<String> HEADER = DataInitializer.headers().get("checkin_records.csv");
    private final CsvStore store;

    public CsvCheckinRecordDao(Path dataDirectory) {
        this.store = new CsvStore(dataDirectory.resolve("checkin_records.csv"), HEADER);
    }

    @Override
    public synchronized void save(CheckinRecord r) {
        List<List<String>> rows = store.all();
        if (store.indexOf(rows, 0, r.getRecordNo()) >= 0) throw new DataAccessException("记录编号已存在：" + r.getRecordNo());
        if (store.indexOf(rows, 1, r.getResNo()) >= 0) throw new DataAccessException("该预约已有签到记录：" + r.getResNo());
        rows.add(List.of(r.getRecordNo(), CsvMapper.text(r.getResNo()),
                CsvMapper.time(r.getActualStart()), CsvMapper.time(r.getActualEnd()),
                CsvMapper.text(r.getPenalty()),
                CsvMapper.time(r.getCreateTime() == null ? new Date() : r.getCreateTime())));
        store.save(rows);
    }

    @Override
    public synchronized void update(CheckinRecord r) {
        List<List<String>> rows = store.all();
        int i = store.indexOf(rows, 1, r.getResNo());
        if (i < 0) throw new DataAccessException("签到记录不存在：" + r.getResNo());
        rows.set(i, List.of(r.getRecordNo(), CsvMapper.text(r.getResNo()),
                CsvMapper.time(r.getActualStart()), CsvMapper.time(r.getActualEnd()),
                CsvMapper.text(r.getPenalty()), CsvMapper.time(r.getCreateTime())));
        store.save(rows);
    }

    @Override
    public synchronized CheckinRecord findByResNo(String resNo) {
        List<List<String>> rows = store.all();
        int i = store.indexOf(rows, 1, resNo);
        if (i < 0) return null;
        return toRecord(rows.get(i));
    }

    @Override
    public synchronized List<CheckinRecord> findAll() {
        List<CheckinRecord> result = new ArrayList<>();
        for (List<String> row : store.data()) {
            result.add(toRecord(row));
        }
        return result;
    }

    private CheckinRecord toRecord(List<String> row) {
        CheckinRecord r = new CheckinRecord(row.get(0), CsvMapper.time(row.get(2)), CsvMapper.time(row.get(3)),
                CsvMapper.integer(row.get(4)));
        r.setResNo(row.get(1));
        r.setCreateTime(CsvMapper.time(row.get(5)));
        return r;
    }
}
