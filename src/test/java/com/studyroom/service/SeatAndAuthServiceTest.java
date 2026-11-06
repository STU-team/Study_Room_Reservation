package com.studyroom.service;

import com.studyroom.dao.SeatDao;
import com.studyroom.dao.UserDao;
import com.studyroom.dao.impl.CsvSeatDao;
import com.studyroom.dao.impl.CsvUserDao;
import com.studyroom.entity.Role;
import com.studyroom.entity.Seat;
import com.studyroom.entity.SeatStatus;
import com.studyroom.entity.User;
import com.studyroom.service.impl.AuthServiceImpl;
import com.studyroom.service.impl.SeatServiceImpl;
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

    @BeforeEach
    void setUp() throws Exception {
        DataInitializer.initialize(dir);
        DataInitializer.seedSamples(dir);
        seatDao = new CsvSeatDao(dir);
        userDao = new CsvUserDao(dir);
        seatService = new SeatServiceImpl(seatDao);
        authService = new AuthServiceImpl(userDao);
    }

    @Test
    void occupiedSeatCannotBeReleasedDirectlyButCanClose() {
        Seat seat = seatService.findByNo("A01");
        seat.updateStatus(SeatStatus.OCCUPIED);
        seatDao.update(seat);

        BusinessException e = assertThrows(BusinessException.class,
                () -> seatService.updateSeatStatus("A01", SeatStatus.AVAILABLE));
        assertEquals("SEAT_IN_USE", e.getCode());
        assertEquals("座位使用中，须先办理签退", e.getMessage());

        seatService.updateSeatStatus("A01", SeatStatus.CLOSED);
        assertEquals(SeatStatus.CLOSED, seatDao.findByNo("A01").getStatus());
    }

    @Test
    void findUnknownSeatThrows() {
        BusinessException e = assertThrows(BusinessException.class, () -> seatService.findByNo("Z99"));
        assertEquals("SEAT_NOT_FOUND", e.getCode());
        assertEquals(4, seatService.listAllSeats().size());
    }

    @Test
    void loginSucceedsThenLogoutClearsSession() {
        User user = authService.login("admin", "123456");
        assertEquals(Role.SYS_ADMIN, user.getRole());
        assertEquals(user, ((com.studyroom.service.impl.AuthServiceImpl) authService).currentUser());

        authService.logout();
        assertNull(((com.studyroom.service.impl.AuthServiceImpl) authService).currentUser());
    }

    @Test
    void loginRejectedOnWrongPasswordOrUnknownUser() {
        BusinessException wrong = assertThrows(BusinessException.class, () -> authService.login("admin", "wrong"));
        assertEquals("AUTH_FAILED", wrong.getCode());
        BusinessException unknown = assertThrows(BusinessException.class, () -> authService.login("nobody", "123456"));
        assertEquals("AUTH_FAILED", unknown.getCode());
    }
}
