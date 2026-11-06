package com.studyroom.dao;

import com.studyroom.dao.impl.CsvCheckinRecordDao;
import com.studyroom.dao.impl.CsvCreditScoreLogDao;
import com.studyroom.dao.impl.CsvReservationDao;
import com.studyroom.dao.impl.CsvSeatDao;
import com.studyroom.dao.impl.CsvSeatTypeDao;
import com.studyroom.dao.impl.CsvStudentDao;
import com.studyroom.dao.impl.CsvUserDao;
import com.studyroom.entity.CheckinRecord;
import com.studyroom.entity.CreditChangeReason;
import com.studyroom.entity.CreditScoreLog;
import com.studyroom.entity.Reservation;
import com.studyroom.entity.ReservationStatus;
import com.studyroom.entity.Role;
import com.studyroom.entity.Seat;
import com.studyroom.entity.SeatStatus;
import com.studyroom.entity.Student;
import com.studyroom.entity.User;
import com.studyroom.util.CsvUtil;
import com.studyroom.util.DataInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 七个 CSV DAO 的读写往返，覆盖契约 v0.8 第八节七张表。 */
class CsvDaoRoundTripTest {
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        DataInitializer.initialize(tempDir);
    }

    @Test
    void seatRoundTripKeepsSeatNoAndStatus() throws Exception {
        Path seats = tempDir.resolve("seats.csv");
        CsvUtil.appendRow(seats, List.of("A01", "靠窗区", "AVAILABLE"));
        CsvUtil.appendRow(seats, List.of("B02", "沙发区", "CLOSED"));

        CsvSeatDao dao = new CsvSeatDao(tempDir);
        assertEquals(2, dao.findAll().size());
        Seat seat = dao.findByNo("A01");
        assertNotNull(seat);
        assertEquals("靠窗区", seat.getRoomName());
        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());

        seat.updateStatus(SeatStatus.OCCUPIED);
        dao.update(seat);
        assertEquals(SeatStatus.OCCUPIED, dao.findByNo("A01").getStatus());

        assertThrows(DataAccessException.class,
                () -> dao.update(new Seat(null, "靠窗区", "C99", SeatStatus.AVAILABLE)));

        CsvSeatTypeDao typeDao = new CsvSeatTypeDao(tempDir, dao);
        assertEquals(1, typeDao.findByTypeName("靠窗区").getSeatCount());
        // seat_types.csv 中的区域即使暂无座位也可见，seatCount 为 0
        CsvUtil.appendRow(tempDir.resolve("seat_types.csv"), List.of("露台区", "0"));
        assertEquals(0, new CsvSeatTypeDao(tempDir, dao).findByTypeName("露台区").getSeatCount());
        assertNull(typeDao.findByTypeName("不存在的区域"));
    }

    @Test
    void reservationRoundTripAndNextResNo() throws Exception {
        Path students = tempDir.resolve("students.csv");
        CsvUtil.appendRow(students, List.of("张三", "20240001", "13800000000", "100"));

        CsvStudentDao studentDao = new CsvStudentDao(tempDir);
        CsvReservationDao dao = new CsvReservationDao(tempDir, studentDao);
        assertTrue(dao.findAll().isEmpty());

        String resNo = dao.nextResNo();
        assertTrue(resNo.startsWith("RES"));

        Date start = at(2026, 11, 6, 8, 0);
        Date end = at(2026, 11, 6, 10, 0);
        Date createTime = at(2026, 11, 3, 20, 0);
        Reservation reservation = new Reservation(null, null, null, createTime, start, end,
                new Date(start.getTime() + 15 * 60_000L), ReservationStatus.BOOKED);
        reservation.setResNo(resNo);
        reservation.setStudentId("20240001");
        reservation.setSeatNo("A01");
        dao.save(reservation);

        Reservation loaded = dao.findByNo(resNo);
        assertNotNull(loaded);
        assertEquals(resNo, loaded.getResNo());
        assertEquals(ReservationStatus.BOOKED, loaded.getStatus());
        assertEquals(start.getTime(), loaded.getStartTime().getTime());
        // createTime 有独立列，与 startTime 不同也能原样还原
        assertEquals(createTime.getTime(), loaded.getCreateTime().getTime());
        assertNotEquals(start.getTime(), loaded.getCreateTime().getTime());
        assertEquals(start.getTime() + 15 * 60_000L, loaded.getSignInDeadline().getTime());
        // resNo 稳定散列得到同一个 reservationId，重启后仍可还原
        assertEquals(loaded.getReservationId(), dao.findByNo(resNo).getReservationId());

        loaded.setStatus(ReservationStatus.CANCELLED);
        dao.update(loaded);
        assertEquals(ReservationStatus.CANCELLED, dao.findByNo(resNo).getStatus());

        assertThrows(DataAccessException.class, () -> dao.update(withResNo(loaded, "RES000000000000")));
    }

    @Test
    void userSaveAndLookup() {
        CsvUserDao dao = new CsvUserDao(tempDir);
        assertNull(dao.findByUsername("admin"));

        dao.save(new User(null, "admin", "123456", "管理员", Role.SYS_ADMIN));
        User loaded = dao.findByUsername("admin");
        assertNotNull(loaded);
        assertEquals("123456", loaded.getPassword());
        assertEquals(Role.SYS_ADMIN, loaded.getRole());
        assertNotNull(loaded.getUserId());

        assertThrows(DataAccessException.class,
                () -> dao.save(new User(null, "admin", "其他", "重复", Role.STUDENT)));
    }

    @Test
    void checkinRecordRoundTripByResNo() {
        CsvCheckinRecordDao dao = new CsvCheckinRecordDao(tempDir);
        assertNull(dao.findByResNo("RES20261106001"));

        CheckinRecord record = new CheckinRecord("REC001", at(2026, 11, 6, 8, 3), at(2026, 11, 6, 10, 12), -1);
        record.setResNo("RES20261106001");
        dao.save(record);

        CheckinRecord loaded = dao.findByResNo("RES20261106001");
        assertNotNull(loaded);
        assertEquals("REC001", loaded.getRecordNo());
        assertEquals(-1, loaded.getPenalty());
        assertNotNull(loaded.getCreateTime());

        CheckinRecord duplicate = new CheckinRecord("REC002", at(2026, 11, 6, 8, 3), null, 0);
        duplicate.setResNo("RES20261106001");
        assertThrows(DataAccessException.class, () -> dao.save(duplicate));

        // 签到即落盘后用 update 回填结束时间与处罚，仍是一条记录
        CheckinRecord open = new CheckinRecord("REC003", at(2026, 11, 6, 8, 5), null, 0);
        open.setResNo("RES20261106002");
        dao.save(open);
        assertNull(dao.findByResNo("RES20261106002").getActualEnd());
        open.setActualEnd(at(2026, 11, 6, 10, 20));
        open.setPenalty(1);
        dao.update(open);
        CheckinRecord closed = dao.findByResNo("RES20261106002");
        assertEquals("REC003", closed.getRecordNo());
        assertNotNull(closed.getActualEnd());
        assertEquals(1, closed.getPenalty());
    }

    @Test
    void creditScoreLogRoundTrip() {
        CsvCreditScoreLogDao dao = new CsvCreditScoreLogDao(tempDir);
        assertTrue(dao.findAll().isEmpty());

        CreditScoreLog noShow = new CreditScoreLog(null, "20240001", 123456789L, -5,
                CreditChangeReason.NO_SHOW, at(2026, 11, 6, 8, 20));
        dao.save(noShow);
        CreditScoreLog late = new CreditScoreLog(null, "20240001", null, -1,
                CreditChangeReason.LATE_CHECKOUT, at(2026, 11, 6, 10, 5));
        dao.save(late);

        List<CreditScoreLog> logs = dao.findByStudentId("20240001");
        assertEquals(2, logs.size());
        assertEquals(-5, logs.get(0).getChangeValue());
        assertEquals(123456789L, logs.get(0).getReservationId());
        assertEquals(CreditChangeReason.LATE_CHECKOUT, logs.get(1).getReason());
        assertNull(logs.get(1).getReservationId());
        assertTrue(logs.get(0).getLogId() < logs.get(1).getLogId());
    }

    private Reservation withResNo(Reservation source, String resNo) {
        Reservation copy = new Reservation(source.getReservationId(), source.getUserId(), source.getSeatId(),
                source.getCreateTime(), source.getStartTime(), source.getEndTime(),
                source.getSignInDeadline(), source.getStatus());
        copy.setResNo(resNo);
        copy.setStudentId(source.getStudentId());
        copy.setSeatNo(source.getSeatNo());
        return copy;
    }

    private Date at(int year, int month, int day, int hour, int minute) {
        return Date.from(LocalDateTime.of(year, month, day, hour, minute)
                .atZone(ZoneId.systemDefault()).toInstant());
    }

    @Test
    void daosShareOneDirectoryAndSeedOnlyFillsEmptyTables() throws Exception {
        new CsvCheckinRecordDao(tempDir);
        new CsvCreditScoreLogDao(tempDir);
        CsvStudentDao studentDao = new CsvStudentDao(tempDir);
        assertTrue(studentDao.findAll().isEmpty());

        DataInitializer.seedSamples(tempDir);
        CsvSeatDao seatDao = new CsvSeatDao(tempDir);
        assertEquals(4, seatDao.findAll().size());
        assertEquals(2, new CsvSeatTypeDao(tempDir, seatDao).findByTypeName("靠窗区").getSeatCount());
        assertEquals(Role.SYS_ADMIN, new CsvUserDao(tempDir).findByUsername("admin").getRole());

        // 再次写入示例数据不重复追加，也不覆盖已有数据
        DataInitializer.seedSamples(tempDir);
        assertEquals(4, seatDao.findAll().size());
    }
}
