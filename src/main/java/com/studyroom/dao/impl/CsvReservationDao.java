package com.studyroom.dao.impl;

import com.studyroom.dao.DataAccessException;
import com.studyroom.dao.ReservationDao;
import com.studyroom.dao.StudentDao;
import com.studyroom.entity.Reservation;
import com.studyroom.entity.ReservationStatus;
import com.studyroom.entity.Student;
import com.studyroom.util.CsvMapper;
import com.studyroom.util.CsvUtil;
import com.studyroom.util.DataInitializer;

import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * ReservationDao 的 CSV 实现。
 *
 * <p>契约 v0.6 的 reservations.csv 为
 * {@code resNo,studentName,studentId,phone,seatNo,resDate,startTime,endTime,status}，
 * 只有业务编号与展示快照，没有 Long 主键与 createTime、signInDeadline。映射约定：</p>
 * <ul>
 *   <li>{@code reservationId} 由 resNo 稳定散列得到，重启后可还原；</li>
 *   <li>{@code userId}、{@code seatId} 分别由 studentId、seatNo 稳定散列得到，与 User/Seat 实体主键一致；</li>
 *   <li>{@code createTime} 表内无列，取 resDate + startTime（预约生成时刻与开始时刻同值）；</li>
 *   <li>{@code signInDeadline} 表内无列，按契约第二节固定规则取 startTime + 15 分钟；</li>
 *   <li>学生姓名与电话是写入时的快照，由 students.csv 按 studentId 反查后填入。</li>
 * </ul>
 */
public class CsvReservationDao implements ReservationDao {
    private static final List<String> HEADER = DataInitializer.headers().get("reservations.csv");
    private static final int COL_RES_NO = 0;
    private static final int COL_STUDENT_NAME = 1;
    private static final int COL_STUDENT_ID = 2;
    private static final int COL_PHONE = 3;
    private static final int COL_SEAT_NO = 4;
    private static final int COL_RES_DATE = 5;
    private static final int COL_START = 6;
    private static final int COL_END = 7;
    private static final int COL_STATUS = 8;

    /** 契约第二节：签到截止时间 = 预约开始时间 + 15 分钟。 */
    private static final int SIGN_IN_DEADLINE_MINUTES = 15;

    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String TIME_PATTERN = "HH:mm";
    private static final String RES_NO_PREFIX = "RES";

    private final Path file;
    private final StudentDao studentDao;

    public CsvReservationDao(Path dataDirectory, StudentDao studentDao) {
        Objects.requireNonNull(dataDirectory, "dataDirectory 不能为空");
        this.studentDao = Objects.requireNonNull(studentDao, "studentDao 不能为空");
        this.file = dataDirectory.resolve("reservations.csv");
        try {
            CsvUtil.ensureHeader(file, HEADER);
        } catch (IOException ex) {
            throw new DataAccessException("初始化 reservations.csv 失败", ex);
        }
    }

    @Override
    public synchronized void save(Reservation reservation) {
        Objects.requireNonNull(reservation, "reservation 不能为空");
        if (reservation.getResNo() == null || reservation.getResNo().isBlank()) {
            reservation.setResNo(nextResNo());
        }
        if (reservation.getStudentId() == null || reservation.getStudentId().isBlank()) {
            throw new IllegalArgumentException("reservation.studentId 不能为空，无法写学生快照");
        }
        if (reservation.getStartTime() == null || reservation.getEndTime() == null) {
            throw new IllegalArgumentException("startTime 与 endTime 不能为空");
        }
        List<List<String>> rows = readRows();
        for (int i = 1; i < rows.size(); i++) {
            if (reservation.getResNo().equals(rows.get(i).get(COL_RES_NO))) {
                throw new DataAccessException("预约号已存在，请改用 update：" + reservation.getResNo());
            }
        }
        rows.add(toRow(reservation));
        writeRows(rows);
    }

    @Override
    public synchronized Reservation findByNo(String resNo) {
        if (resNo == null || resNo.isBlank()) {
            return null;
        }
        List<List<String>> rows = readRows();
        for (int i = 1; i < rows.size(); i++) {
            if (resNo.equals(rows.get(i).get(COL_RES_NO))) {
                return toReservation(rows.get(i));
            }
        }
        return null;
    }

    @Override
    public synchronized List<Reservation> findAll() {
        List<List<String>> rows = readRows();
        List<Reservation> reservations = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            reservations.add(toReservation(rows.get(i)));
        }
        return reservations;
    }

    @Override
    public synchronized void update(Reservation reservation) {
        Objects.requireNonNull(reservation, "reservation 不能为空");
        if (reservation.getResNo() == null || reservation.getResNo().isBlank()) {
            throw new IllegalArgumentException("更新预约必须带 resNo");
        }
        List<List<String>> rows = readRows();
        int found = -1;
        for (int i = 1; i < rows.size(); i++) {
            if (reservation.getResNo().equals(rows.get(i).get(COL_RES_NO))) {
                if (found >= 0) {
                    throw new DataAccessException("reservations.csv 中存在重复预约号：" + reservation.getResNo());
                }
                found = i;
            }
        }
        if (found < 0) {
            throw new DataAccessException("待更新的预约不存在：" + reservation.getResNo());
        }
        rows.set(found, toRow(reservation));
        writeRows(rows);
    }

    /** 生成当天下一个预约号：RES + yyyyMMdd + 三位序号，与契约示例一致。 */
    @Override
    public synchronized String nextResNo() {
        String today = new SimpleDateFormat(DATE_PATTERN, Locale.ROOT).format(new Date());
        int max = 0;
        List<List<String>> rows = readRows();
        for (int i = 1; i < rows.size(); i++) {
            String resNo = rows.get(i).get(COL_RES_NO);
            if (resNo != null && resNo.startsWith(RES_NO_PREFIX + today)) {
                String suffix = resNo.substring((RES_NO_PREFIX + today).length());
                try {
                    max = Math.max(max, Integer.parseInt(suffix));
                } catch (NumberFormatException ignored) {
                    // 非约定格式的历史编号不参与序号计算
                }
            }
        }
        return RES_NO_PREFIX + today + String.format("%03d", max + 1);
    }

    private List<String> toRow(Reservation reservation) {
        String resDate = new SimpleDateFormat(DATE_PATTERN, Locale.ROOT).format(reservation.getStartTime());
        String start = new SimpleDateFormat(TIME_PATTERN, Locale.ROOT).format(reservation.getStartTime());
        String end = new SimpleDateFormat(TIME_PATTERN, Locale.ROOT).format(reservation.getEndTime());
        String studentName = "";
        String phone = "";
        Student student = studentDao.findByStudentId(reservation.getStudentId());
        if (student != null) {
            studentName = CsvMapper.text(student.getName());
            phone = CsvMapper.text(student.getPhone());
        }
        return List.of(reservation.getResNo(), studentName, reservation.getStudentId(), phone,
                seatNoOf(reservation), resDate, start, end, reservation.getStatus().name());
    }

    private Reservation toReservation(List<String> row) {
        if (row.size() != HEADER.size()) {
            throw new DataAccessException("reservations.csv 行字段数量错误：" + row);
        }
        String resNo = row.get(COL_RES_NO);
        Date start = combine(row.get(COL_RES_DATE), row.get(COL_START));
        Date end = combine(row.get(COL_RES_DATE), row.get(COL_END));
        Date signInDeadline = new Date(start.getTime() + SIGN_IN_DEADLINE_MINUTES * 60_000L);
        ReservationStatus status;
        try {
            status = ReservationStatus.valueOf(row.get(COL_STATUS).trim());
        } catch (IllegalArgumentException ex) {
            throw new DataAccessException("reservations.csv 预约状态非法：" + row.get(COL_STATUS), ex);
        }
        Reservation reservation = new Reservation(
                CsvMapper.stableId(resNo),
                CsvMapper.stableId(row.get(COL_STUDENT_ID)),
                CsvMapper.stableId(row.get(COL_SEAT_NO)),
                start, start, end, signInDeadline, status);
        reservation.setResNo(resNo);
        reservation.setStudentId(row.get(COL_STUDENT_ID));
        reservation.setSeatNo(row.get(COL_SEAT_NO));
        return reservation;
    }

    /**
     * 预约实体的 seatId 已在构造时按 seatNo 散列，写回 CSV 时只需按同一规则反查座位号。
     * 为此保留最近一次读入的编号映射，避免出现有损的逆散列。
     */
    private String seatNoOf(Reservation reservation) {
        if (reservation.getSeatNo() != null) {
            return reservation.getSeatNo();
        }
        throw new DataAccessException("reservation.seatNo 不能为空，seatId 无法反查座位号");
    }

    private Date combine(String date, String time) {
        try {
            return new SimpleDateFormat(DATE_PATTERN + " " + TIME_PATTERN, Locale.ROOT)
                    .parse(date.trim() + " " + time.trim());
        } catch (Exception ex) {
            throw new DataAccessException("reservations.csv 日期时间格式错误：" + date + " " + time, ex);
        }
    }

    private List<List<String>> readRows() {
        try {
            List<List<String>> rows = CsvUtil.readAll(file);
            if (rows.isEmpty() || !HEADER.equals(rows.get(0))) {
                throw new DataAccessException("reservations.csv 表头缺失或与契约不一致");
            }
            return rows;
        } catch (IOException ex) {
            throw new DataAccessException("读取 reservations.csv 失败", ex);
        }
    }

    private void writeRows(List<List<String>> rows) {
        try {
            CsvUtil.writeAll(file, rows);
        } catch (IOException ex) {
            throw new DataAccessException("写入 reservations.csv 失败", ex);
        }
    }
}
