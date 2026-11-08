package com.studyroom.entity;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class EntityModelTest {

    @Test
    void enumValuesMustMatchContractV06() {
        assertArrayEquals(new Role[]{Role.STUDENT, Role.DUTY_ADMIN, Role.SYS_ADMIN}, Role.values());
        assertArrayEquals(new SeatStatus[]{SeatStatus.AVAILABLE, SeatStatus.RESERVED,
                SeatStatus.OCCUPIED, SeatStatus.CLOSED}, SeatStatus.values());
        assertArrayEquals(new ReservationStatus[]{ReservationStatus.BOOKED,
                ReservationStatus.CHECKED_IN, ReservationStatus.CHECKED_OUT,
                ReservationStatus.CANCELLED, ReservationStatus.NO_SHOW}, ReservationStatus.values());
        assertArrayEquals(new CreditChangeReason[]{CreditChangeReason.NO_SHOW,
                CreditChangeReason.LATE_CHECKOUT, CreditChangeReason.OTHER}, CreditChangeReason.values());
    }

    @Test
    void userAndStudentKeepIdentityAndCreditInSeparateEntities() {
        User user = new User(10L, "stu001", "secret", "张三", Role.STUDENT);
        Student student = new Student("20240001", "张三", "13800000000", 100);

        assertEquals(10L, user.getUserId());
        assertEquals(Role.STUDENT, user.getRole());
        assertEquals("20240001", student.getStudentId());
        assertEquals(100, student.getCreditScore());
    }

    @Test
    void reservationStateTransitionsFollowReservationStateDiagram() {
        Reservation reservation = new Reservation();
        reservation.setStatus(ReservationStatus.BOOKED);

        assertTrue(reservation.checkIn());
        assertEquals(ReservationStatus.CHECKED_IN, reservation.getStatus());
        assertFalse(reservation.cancel());
        assertTrue(reservation.checkOut());
        assertEquals(ReservationStatus.CHECKED_OUT, reservation.getStatus());
        assertFalse(reservation.checkOut());
    }

    @Test
    void cancellationAndNoShowRemainDifferentTerminalStates() {
        Reservation cancelled = new Reservation();
        cancelled.setStatus(ReservationStatus.BOOKED);
        assertTrue(cancelled.cancel());
        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());

        Reservation noShow = new Reservation();
        noShow.setStatus(ReservationStatus.BOOKED);
        noShow.handleNoShow();
        assertEquals(ReservationStatus.NO_SHOW, noShow.getStatus());
    }

    @Test
    void datePropertiesAreDefensivelyCopied() {
        Date input = new Date(1_800_000_000_000L);
        Reservation reservation = new Reservation(1L, 2L, 3L, input, input, input, input,
                ReservationStatus.BOOKED);
        input.setTime(0L);

        Date returned = reservation.getStartTime();
        assertEquals(1_800_000_000_000L, returned.getTime());
        returned.setTime(0L);
        assertEquals(1_800_000_000_000L, reservation.getStartTime().getTime());
    }

    @Test
    void creditScoreLogAllowsNullReservationIdForManualAdjustment() {
        CreditScoreLog log = new CreditScoreLog(1L, "20240001", null, 3,
                CreditChangeReason.OTHER, new Date());

        assertNull(log.getReservationId());
        assertEquals(3, log.getChangeValue());
        assertEquals(CreditChangeReason.OTHER, log.getReason());
    }
}
