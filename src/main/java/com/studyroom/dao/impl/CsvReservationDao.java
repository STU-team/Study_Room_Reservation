package com.studyroom.dao.impl;

import com.studyroom.dao.DataAccessException;
import com.studyroom.dao.ReservationDao;
import com.studyroom.dao.StudentDao;
import com.studyroom.entity.Reservation;
import com.studyroom.entity.ReservationStatus;
import com.studyroom.entity.Student;
import com.studyroom.util.CsvMapper;
import com.studyroom.util.CsvStore;
import com.studyroom.util.DataInitializer;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * ReservationDao 的 CSV 实现。
 * 映射：reservationId 由 resNo 散列，userId/seatId 由 studentId/seatNo 散列；
 * createTime 有独立列（契约 v0.8），signInDeadline 取开始时间+15 分钟（契约第二节）。
 */
public class CsvReservationDao implements ReservationDao {
    private static final List<String> HEADER = DataInitializer.headers().get("reservations.csv");
    private static final int MINUTES_DEADLINE = 15;
    private static final String DATE = "yyyy-MM-dd";
    /** 预约号只取紧凑日期，RES + yyyyMMdd + 三位序号（契约第一节）。 */
    private static final String NO_DATE = "yyyyMMdd";
    private static final String TIME = "HH:mm";
    private final CsvStore store;
    private final StudentDao studentDao;

    public CsvReservationDao(Path dataDirectory, StudentDao studentDao) {
        this.store = new CsvStore(dataDirectory.resolve("reservations.csv"), HEADER);
        this.studentDao = Objects.requireNonNull(studentDao, "studentDao 不能为空");
    }

    @Override
    public synchronized void save(Reservation r) {
        if (r.getStudentId() == null || r.getSeatNo() == null || r.getStartTime() == null || r.getEndTime() == null) {
            throw new IllegalArgumentException("预约缺少 studentId/seatNo/startTime/endTime");
        }
        List<List<String>> rows = store.all();
        if (r.getResNo() == null || r.getResNo().isBlank()) r.setResNo(nextResNo(rows));
        if (store.indexOf(rows, 0, r.getResNo()) >= 0) throw new DataAccessException("预约号已存在：" + r.getResNo());
        rows.add(toRow(r));
        store.save(rows);
    }

    @Override
    public synchronized Reservation findByNo(String resNo) {
        List<List<String>> rows = store.all();
        int i = store.indexOf(rows, 0, resNo);
        return i < 0 ? null : toReservation(rows.get(i));
    }

    @Override
    public synchronized List<Reservation> findAll() {
        List<Reservation> list = new ArrayList<>();
        for (List<String> row : store.data()) list.add(toReservation(row));
        return list;
    }

    @Override
    public synchronized void update(Reservation r) {
        List<List<String>> rows = store.all();
        int i = store.indexOf(rows, 0, r.getResNo());
        if (i < 0) throw new DataAccessException("待更新的预约不存在：" + r.getResNo());
        rows.set(i, toRow(r));
        store.save(rows);
    }

    /** 当天最大序号 +1：RES + yyyyMMdd + 三位（契约示例格式）。 */
    @Override
    public synchronized String nextResNo() {
        return nextResNo(store.all());
    }

    /** 复用已读入的整表，避免 save 时重复读盘。 */
    private String nextResNo(List<List<String>> rows) {
        String prefix = "RES" + new SimpleDateFormat(NO_DATE, Locale.ROOT).format(new Date());
        int max = 0;
        for (int i = 1; i < rows.size(); i++) {
            String no = rows.get(i).get(0);
            if (no.startsWith(prefix)) max = Math.max(max, parse(no.substring(prefix.length())));
        }
        return prefix + String.format("%03d", max + 1);
    }

    private int parse(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private List<String> toRow(Reservation r) {
        String date = new SimpleDateFormat(DATE, Locale.ROOT).format(r.getStartTime());
        String start = new SimpleDateFormat(TIME, Locale.ROOT).format(r.getStartTime());
        String end = new SimpleDateFormat(TIME, Locale.ROOT).format(r.getEndTime());
        Student s = studentDao.findByStudentId(r.getStudentId());
        String created = CsvMapper.time(r.getCreateTime() == null ? r.getStartTime() : r.getCreateTime());
        return List.of(r.getResNo(), s == null ? "" : CsvMapper.text(s.getName()), r.getStudentId(),
                s == null ? "" : CsvMapper.text(s.getPhone()), r.getSeatNo(), date, start, end,
                r.getStatus().name(), created);
    }

    private Reservation toReservation(List<String> row) {
        String date = row.get(5);
        String start = row.get(6);
        String end = row.get(7);
        Date startAt = combine(date, start);
        Date createTime = CsvMapper.time(row.get(9));
        Reservation r = new Reservation(
                CsvMapper.id(row.get(0)),
                CsvMapper.id(row.get(2)),
                CsvMapper.id(row.get(4)),
                createTime == null ? startAt : createTime, startAt, combine(date, end),
                new Date(startAt.getTime() + MINUTES_DEADLINE * 60_000L),
                ReservationStatus.valueOf(row.get(8).isBlank() ? "BOOKED" : row.get(8)));
        r.setResNo(row.get(0));
        r.setStudentId(row.get(2));
        r.setStudentName(row.get(1));
        r.setPhone(row.get(3));
        r.setSeatNo(row.get(4));
        return r;
    }

    private Date combine(String date, String time) {
        try {
            return new SimpleDateFormat(DATE + " " + TIME, Locale.ROOT).parse(date.trim() + " " + time.trim());
        } catch (Exception e) {
            throw new DataAccessException("日期时间格式错误：" + date + " " + time, e);
        }
    }
}
