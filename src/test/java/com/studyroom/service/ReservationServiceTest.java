package com.studyroom.service;

import com.studyroom.dao.CheckinRecordDao;
import com.studyroom.dao.CreditScoreLogDao;
import com.studyroom.dao.ReservationDao;
import com.studyroom.dao.SeatDao;
import com.studyroom.dao.StudentDao;
import com.studyroom.dao.impl.CsvCheckinRecordDao;
import com.studyroom.dao.impl.CsvCreditScoreLogDao;
import com.studyroom.dao.impl.CsvReservationDao;
import com.studyroom.dao.impl.CsvSeatDao;
import com.studyroom.dao.impl.CsvStudentDao;
import com.studyroom.entity.CheckinRecord;
import com.studyroom.entity.CreditChangeReason;
import com.studyroom.entity.CreditScoreLog;
import com.studyroom.entity.Reservation;
import com.studyroom.entity.ReservationStatus;
import com.studyroom.entity.Seat;
import com.studyroom.entity.SeatStatus;
import com.studyroom.entity.Student;
import com.studyroom.service.impl.ReservationServiceImpl;
import com.studyroom.util.DataInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReservationServiceTest {

    @TempDir
    Path dir;

    private SeatDao seatDao;
    private ReservationDao reservationDao;
    private StudentDao studentDao;
    private CheckinRecordDao checkinRecordDao;
    private CreditScoreLogDao creditScoreLogDao;
    private ReservationService service;

    private static final LocalDate TOMORROW = LocalDate.now().plusDays(1);
    private static final LocalTime TEN = LocalTime.of(10, 0);
    private static final LocalTime TWELVE = LocalTime.of(12, 0);

    @BeforeEach
    void setUp() throws Exception {
        DataInitializer.initialize(dir);
        DataInitializer.seedSamples(dir);
        seatDao = new CsvSeatDao(dir);
        studentDao = new CsvStudentDao(dir);
        checkinRecordDao = new CsvCheckinRecordDao(dir);
        creditScoreLogDao = new CsvCreditScoreLogDao(dir);
        reservationDao = new CsvReservationDao(dir, studentDao);
        service = new ReservationServiceImpl(seatDao, reservationDao, studentDao, checkinRecordDao, creditScoreLogDao);
    }

    @Test
    void reserveLocksSeatAndSetsSignInDeadline() {
        Reservation r = book("A01", TEN, TWELVE);

        assertEquals(ReservationStatus.BOOKED, r.getStatus());
        assertTrue(r.getResNo().matches("RES\\d{8}001"));
        assertEquals(SeatStatus.RESERVED, seatDao.findByNo("A01").getStatus());
        assertEquals(LocalTime.of(10, 15), com.studyroom.util.DateTimes.time(r.getSignInDeadline()));
        assertEquals(ReservationStatus.BOOKED, reservationDao.findByNo(r.getResNo()).getStatus());
    }

    @Test
    void reserveRejectedWhenCreditBelowThreshold() {
        Student student = studentDao.findByStudentId("20240001");
        student.setCreditScore(79);
        studentDao.update(student);

        BusinessException e = assertThrows(BusinessException.class, () -> book("A01", TEN, TWELVE));
        assertEquals("CREDIT_LOW", e.getCode());
        assertEquals(SeatStatus.AVAILABLE, seatDao.findByNo("A01").getStatus());
    }

    @Test
    void reserveRejectedWhenSeatNotAvailable() {
        book("A01", TEN, TWELVE);
        BusinessException occupied = assertThrows(BusinessException.class, () -> book("A01", TEN, TWELVE));
        assertEquals("SEAT_UNAVAILABLE", occupied.getCode());

        Seat a02 = seatDao.findByNo("A02");
        a02.updateStatus(SeatStatus.CLOSED);
        seatDao.update(a02);
        BusinessException closed = assertThrows(BusinessException.class, () -> book("A02", TEN, TWELVE));
        assertEquals("SEAT_UNAVAILABLE", closed.getCode());
    }

    @Test
    void listAvailableSeatsFiltersOccupiedSlotAndArea() {
        book("A01", TEN, TWELVE);

        List<Seat> window = service.listAvailableSeats(TOMORROW, TEN, TWELVE, "靠窗区");
        assertEquals(List.of("A02"), window.stream().map(Seat::getSeatNo).toList());

        List<Seat> anyArea = service.listAvailableSeats(TOMORROW, TEN, TWELVE, null);
        assertEquals(3, anyArea.size());

        List<Seat> otherSlot = service.listAvailableSeats(TOMORROW, LocalTime.of(13, 0), LocalTime.of(15, 0), "靠窗区");
        assertEquals(1, otherSlot.size());
    }

    @Test
    void cancelReleasesSeatAndOnlyWorksOnce() {
        Reservation r = book("A01", TEN, TWELVE);
        Reservation cancelled = service.cancelReservation(r.getResNo());

        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(SeatStatus.AVAILABLE, seatDao.findByNo("A01").getStatus());
        BusinessException again = assertThrows(BusinessException.class,
                () -> service.cancelReservation(r.getResNo()));
        assertEquals("STATE", again.getCode());
    }

    @Test
    void signInWritesRecordAndSignOutUpdatesSameRecord() {
        Reservation r = book("A01", TEN, TWELVE);
        service.signIn(r.getResNo());

        assertEquals(ReservationStatus.CHECKED_IN, reservationDao.findByNo(r.getResNo()).getStatus());
        assertEquals(SeatStatus.OCCUPIED, seatDao.findByNo("A01").getStatus());

        CheckinRecord signedIn = checkinRecordDao.findByResNo(r.getResNo());
        assertNotNull(signedIn);
        assertNull(signedIn.getActualEnd());
        assertEquals(0, signedIn.getPenalty());

        CheckinRecord record = service.signOut(r.getResNo(), LocalTime.of(11, 50));
        assertEquals(signedIn.getRecordNo(), record.getRecordNo());
        assertEquals(0, record.getPenalty());
        assertNotNull(checkinRecordDao.findByResNo(r.getResNo()).getActualEnd());
        assertEquals(SeatStatus.AVAILABLE, seatDao.findByNo("A01").getStatus());
        assertEquals(ReservationStatus.CHECKED_OUT, reservationDao.findByNo(r.getResNo()).getStatus());
    }

    @Test
    void signOutAfterEndDeductsOnePoint() {
        Reservation r = book("A01", TEN, TWELVE);
        service.signIn(r.getResNo());
        CheckinRecord record = service.signOut(r.getResNo(), LocalTime.of(12, 30));

        assertEquals(1, record.getPenalty());
        assertEquals(99, studentDao.findByStudentId("20240001").getCreditScore());
        List<CreditScoreLog> logs = creditScoreLogDao.findByStudentId("20240001");
        assertEquals(CreditChangeReason.LATE_CHECKOUT, logs.get(logs.size() - 1).getReason());
        assertEquals(-1, logs.get(logs.size() - 1).getChangeValue());
    }

    @Test
    void handleNoShowMarksNoShowReleasesSeatAndDeductsFive() {
        Reservation r = book("A01", TEN, TWELVE);

        int handled = service.handleNoShow(LocalDateTime.of(TOMORROW, LocalTime.of(10, 20)));

        assertEquals(1, handled);
        assertEquals(ReservationStatus.NO_SHOW, reservationDao.findByNo(r.getResNo()).getStatus());
        assertEquals(SeatStatus.AVAILABLE, seatDao.findByNo("A01").getStatus());
        assertEquals(95, studentDao.findByStudentId("20240001").getCreditScore());
        CreditScoreLog log = creditScoreLogDao.findByStudentId("20240001").get(0);
        assertEquals(CreditChangeReason.NO_SHOW, log.getReason());
        assertEquals(-5, log.getChangeValue());
        assertEquals(0, service.handleNoShow(LocalDateTime.of(TOMORROW, LocalTime.of(11, 20))));
    }

    @Test
    void reserveRejectedWhenDateOutsideWindow() {
        Student student = studentDao.findByStudentId("20240001");

        BusinessException past = assertThrows(BusinessException.class,
                () -> service.reserve("A01", student, LocalDate.now().minusDays(1), TEN, TWELVE));
        assertEquals("PARAM", past.getCode());

        BusinessException far = assertThrows(BusinessException.class,
                () -> service.reserve("A01", student, LocalDate.now().plusDays(8), TEN, TWELVE));
        assertEquals("PARAM", far.getCode());
        assertEquals(SeatStatus.AVAILABLE, seatDao.findByNo("A01").getStatus());
    }

    @Test
    void listReservationsFiltersByStudentAndOrdersByStartDesc() {
        Reservation morning = book("A02", LocalTime.of(8, 0), LocalTime.of(10, 0));
        Reservation noon = book("A01", TEN, TWELVE);

        List<Reservation> mine = service.listReservations("20240001");
        assertEquals(2, mine.size());
        assertEquals(noon.getResNo(), mine.get(0).getResNo());
        assertEquals(morning.getResNo(), mine.get(1).getResNo());

        assertEquals(0, service.listReservations("20240002").size());
        assertEquals(2, service.listAllReservations().size());
        assertEquals("PARAM", assertThrows(BusinessException.class, () -> service.listReservations(" ")).getCode());
    }

    @Test
    void listSeatedReturnsOnlyOpenRecords() {
        Reservation r = book("A01", TEN, TWELVE);
        assertTrue(service.listSeated().isEmpty());

        service.signIn(r.getResNo());
        assertEquals(1, service.listSeated().size());
        assertEquals(r.getResNo(), service.listSeated().get(0).getResNo());

        service.signOut(r.getResNo(), LocalTime.of(11, 50));
        assertTrue(service.listSeated().isEmpty());
        assertEquals(1, checkinRecordDao.findAll().size());
    }

    private Reservation book(String seatNo, LocalTime start, LocalTime end) {
        Student student = studentDao.findByStudentId("20240001");
        return service.reserve(seatNo, student, TOMORROW, start, end);
    }
}
