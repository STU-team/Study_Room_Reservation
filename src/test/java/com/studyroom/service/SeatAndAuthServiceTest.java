package com.studyroom.service;

import com.studyroom.dao.SeatDao;
import com.studyroom.dao.UserDao;
import com.studyroom.dao.impl.CsvSeatDao;
import com.studyroom.dao.impl.CsvStudentDao;
import com.studyroom.dao.impl.CsvUserDao;
import com.studyroom.entity.Role;
import com.studyroom.entity.Seat;
import com.studyroom.entity.SeatStatus;
import com.studyroom.entity.User;
import com.studyroom.service.impl.AuthServiceImpl;
import com.studyroom.service.impl.SeatServiceImpl;
import com.studyroom.service.impl.StudentServiceImpl;
import com.studyroom.util.DataInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SeatAndAuthServiceTest {

    @TempDir
    Path dir;

    private SeatDao seatDao;
    private UserDao userDao;
    private SeatService seatService;
    private AuthService authService;
    private StudentService studentService;

    @BeforeEach
    void setUp() throws Exception {
        DataInitializer.initialize(dir);
        DataInitializer.seedSamples(dir);
        seatDao = new CsvSeatDao(dir);
        userDao = new CsvUserDao(dir);
        seatService = new SeatServiceImpl(seatDao);
        authService = new AuthServiceImpl(userDao);
        studentService = new StudentServiceImpl(new CsvStudentDao(dir));
    }

    @Test
    void manualStatusMatrixEnforced() {
        Seat a01 = seatService.findByNo("A01");

        // AVAILABLE <-> CLOSED 允许
        seatService.updateSeatStatus("A01", SeatStatus.CLOSED);
        assertEquals(SeatStatus.CLOSED, seatDao.findByNo("A01").getStatus());
        seatService.updateSeatStatus("A01", SeatStatus.AVAILABLE);
        assertEquals(SeatStatus.AVAILABLE, seatDao.findByNo("A01").getStatus());

        // OCCUPIED 不得手动改为任何状态（含 CLOSED）
        a01.updateStatus(SeatStatus.OCCUPIED);
        seatDao.update(a01);
        BusinessException toAvail = assertThrows(BusinessException.class,
                () -> seatService.updateSeatStatus("A01", SeatStatus.AVAILABLE));
        assertEquals("SEAT_IN_USE", toAvail.getCode());
        BusinessException toClosed = assertThrows(BusinessException.class,
                () -> seatService.updateSeatStatus("A01", SeatStatus.CLOSED));
        assertEquals("SEAT_IN_USE", toClosed.getCode());
        assertEquals(SeatStatus.OCCUPIED, seatDao.findByNo("A01").getStatus());

        // RESERVED 不得手动改为任何状态
        Seat a02 = seatService.findByNo("A02");
        a02.updateStatus(SeatStatus.RESERVED);
        seatDao.update(a02);
        BusinessException reserved = assertThrows(BusinessException.class,
                () -> seatService.updateSeatStatus("A02", SeatStatus.CLOSED));
        assertEquals("STATE", reserved.getCode());
        BusinessException reservedToOcc = assertThrows(BusinessException.class,
                () -> seatService.updateSeatStatus("A02", SeatStatus.OCCUPIED));
        assertEquals("STATE", reservedToOcc.getCode());

        // AVAILABLE 不能手动改成 OCCUPIED（使用中只能由签到产生）
        a02.updateStatus(SeatStatus.AVAILABLE);
        seatDao.update(a02);
        BusinessException toOcc = assertThrows(BusinessException.class,
                () -> seatService.updateSeatStatus("A02", SeatStatus.OCCUPIED));
        assertEquals("STATE", toOcc.getCode());
        BusinessException availToReserved = assertThrows(BusinessException.class,
                () -> seatService.updateSeatStatus("A02", SeatStatus.RESERVED));
        assertEquals("STATE", availToReserved.getCode());
    }

    @Test
    void findUnknownSeatThrows() {
        BusinessException e = assertThrows(BusinessException.class, () -> seatService.findByNo("Z99"));
        assertEquals("SEAT_NOT_FOUND", e.getCode());
        assertEquals(4, seatService.listAllSeats().size());
    }

    @Test
    void loginReturnsUserAndLogoutKeepsServiceStateless() {
        User admin = authService.login("admin", "123456");
        assertEquals(Role.SYS_ADMIN, admin.getRole());

        authService.logout();

        User duty = authService.login("duty", "123456");
        assertEquals(Role.DUTY_ADMIN, duty.getRole());
        assertEquals("admin", admin.getUsername());
    }

    @Test
    void studentServiceReturnsCreditScoreAndRejectsUnknownId() {
        assertEquals(100, studentService.findByStudentId("20240001").getCreditScore());
        assertEquals("张三", studentService.findByStudentId("20240001").getName());

        BusinessException e = assertThrows(BusinessException.class,
                () -> studentService.findByStudentId("20249999"));
        assertEquals("STUDENT_NOT_FOUND", e.getCode());
        assertEquals("PARAM", assertThrows(BusinessException.class, () -> studentService.findByStudentId(" ")).getCode());
        assertEquals(2, studentService.listStudents().size());
    }

    @Test
    void loginRejectedOnWrongPasswordOrUnknownUser() {
        BusinessException wrong = assertThrows(BusinessException.class, () -> authService.login("admin", "wrong"));
        assertEquals("AUTH_FAILED", wrong.getCode());
        BusinessException unknown = assertThrows(BusinessException.class, () -> authService.login("nobody", "123456"));
        assertEquals("AUTH_FAILED", unknown.getCode());
    }
}
