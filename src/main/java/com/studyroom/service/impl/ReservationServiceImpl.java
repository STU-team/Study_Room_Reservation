package com.studyroom.service.impl;

import com.studyroom.dao.CheckinRecordDao;
import com.studyroom.dao.CreditScoreLogDao;
import com.studyroom.dao.ReservationDao;
import com.studyroom.dao.SeatDao;
import com.studyroom.dao.StudentDao;
import com.studyroom.entity.CheckinRecord;
import com.studyroom.entity.CreditChangeReason;
import com.studyroom.entity.CreditScoreLog;
import com.studyroom.entity.Reservation;
import com.studyroom.entity.ReservationStatus;
import com.studyroom.entity.Seat;
import com.studyroom.entity.SeatStatus;
import com.studyroom.entity.Student;
import com.studyroom.service.BusinessException;
import com.studyroom.service.ReservationService;
import com.studyroom.util.DateTimes;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 预约业务实现：按契约第一/二节与 A-4 状态图 R-01~R-05、T-02~T-05 校验与迁移。 */
public class ReservationServiceImpl implements ReservationService {
    private static final int MIN_CREDIT = 80;
    private static final int DEADLINE_MINUTES = 15;
    private static final int MAX_AHEAD_DAYS = 7;
    private static final int NO_SHOW_PENALTY = -5;
    private static final int LATE_PENALTY = -1;

    private final SeatDao seatDao;
    private final ReservationDao reservationDao;
    private final StudentDao studentDao;
    private final CheckinRecordDao checkinRecordDao;
    private final CreditScoreLogDao creditScoreLogDao;

    public ReservationServiceImpl(SeatDao seatDao, ReservationDao reservationDao, StudentDao studentDao,
                                  CheckinRecordDao checkinRecordDao, CreditScoreLogDao creditScoreLogDao) {
        this.seatDao = seatDao;
        this.reservationDao = reservationDao;
        this.studentDao = studentDao;
        this.checkinRecordDao = checkinRecordDao;
        this.creditScoreLogDao = creditScoreLogDao;
    }

    @Override
    public List<Seat> listAvailableSeats(LocalDate date, LocalTime start, LocalTime end, String typeName) {
        if (date == null || start == null || end == null) throw new BusinessException("PARAM", "查询时段不能为空");
        Date from = DateTimes.at(date, start);
        Date to = DateTimes.at(date, end);
        if (!to.after(from)) throw new BusinessException("PARAM", "结束时间必须晚于开始时间");
        List<Reservation> reservations = reservationDao.findAll();
        List<Seat> result = new ArrayList<>();
        for (Seat seat : seatDao.findAll()) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) continue;
            if (typeName != null && !typeName.isBlank() && !typeName.equals(seat.getRoomName())) continue;
            if (occupied(reservations, seat.getSeatNo(), from, to)) continue;
            result.add(seat);
        }
        return result;
    }

    @Override
    public List<Reservation> listReservations(String studentId) {
        if (studentId == null || studentId.isBlank()) throw new BusinessException("PARAM", "学号不能为空");
        List<Reservation> result = new ArrayList<>();
        for (Reservation r : reservationDao.findAll()) {
            if (studentId.equals(r.getStudentId())) result.add(r);
        }
        return sortByStartDesc(result);
    }

    @Override
    public List<Reservation> listAllReservations() {
        return sortByStartDesc(reservationDao.findAll());
    }

    @Override
    public List<CheckinRecord> listSeated() {
        List<CheckinRecord> result = new ArrayList<>();
        for (CheckinRecord record : checkinRecordDao.findAll()) {
            if (record.getActualEnd() == null) result.add(record);
        }
        return result;
    }

    @Override
    public Reservation findReservation(String resNo) {
        return require(resNo);
    }

    @Override
    public Reservation reserve(String seatNo, Student student, LocalDate date, LocalTime start, LocalTime end) {
        if (seatNo == null || student == null || date == null || start == null || end == null) {
            throw new BusinessException("PARAM", "预约信息不完整");
        }
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) throw new BusinessException("PARAM", "不能预约过去的日期");
        if (date.isAfter(today.plusDays(MAX_AHEAD_DAYS))) {
            throw new BusinessException("PARAM", "只能预约 7 天内的日期");
        }
        Student current = studentDao.findByStudentId(student.getStudentId());
        if (current == null) throw new BusinessException("STUDENT_NOT_FOUND", "学生档案不存在：" + student.getStudentId());
        if (current.getCreditScore() == null || current.getCreditScore() < MIN_CREDIT) {
            throw new BusinessException("CREDIT_LOW", "信用分低于 80，暂不能预约");
        }
        Date from = DateTimes.at(date, start);
        Date to = DateTimes.at(date, end);
        if (!to.after(from)) throw new BusinessException("PARAM", "结束时间必须晚于开始时间");
        Seat seat = seatDao.findByNo(seatNo);
        if (seat == null) throw new BusinessException("SEAT_NOT_FOUND", "座位不存在：" + seatNo);
        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            throw new BusinessException("SEAT_UNAVAILABLE", "座位当前不可预约：" + seat.getStatus());
        }
        if (occupied(reservationDao.findAll(), seatNo, from, to)) {
            throw new BusinessException("SEAT_CONFLICT", "该座位在所选时段已被预约");
        }

        Reservation reservation = new Reservation(null, null, null, new Date(), from, to,
                DateTimes.plusMinutes(from, DEADLINE_MINUTES), ReservationStatus.BOOKED);
        reservation.setResNo(reservationDao.nextResNo());
        reservation.setStudentId(current.getStudentId());
        reservation.setStudentName(current.getName());
        reservation.setPhone(current.getPhone());
        reservation.setSeatNo(seatNo);
        reservationDao.save(reservation);

        seat.updateStatus(SeatStatus.RESERVED);
        seatDao.update(seat);
        return reservation;
    }

    @Override
    public Reservation cancelReservation(String resNo) {
        Reservation reservation = require(resNo);
        if (reservation.getStatus() != ReservationStatus.BOOKED) {
            throw new BusinessException("STATE", "只有已预约未签到的记录可以取消");
        }
        if (new Date().after(reservation.getSignInDeadline())) {
            throw new BusinessException("DEADLINE", "已超过签到截止时间，无法取消");
        }
        reservation.cancel();
        reservationDao.update(reservation);
        release(reservation.getSeatNo(), SeatStatus.RESERVED);
        return reservation;
    }

    @Override
    public Reservation signIn(String resNo) {
        Reservation reservation = require(resNo);
        if (reservation.getStatus() != ReservationStatus.BOOKED) {
            throw new BusinessException("STATE", "只有已预约未签到的记录可以签到");
        }
        if (new Date().after(reservation.getSignInDeadline())) {
            throw new BusinessException("DEADLINE", "已超过签到截止时间，无法签到");
        }
        reservation.checkIn();
        reservationDao.update(reservation);
        setSeatStatus(reservation.getSeatNo(), SeatStatus.OCCUPIED);
        checkinRecordDao.save(newRecord(resNo, new Date(), null, 0));
        return reservation;
    }

    @Override
    public CheckinRecord signOut(String resNo, LocalTime actualEnd) {
        if (actualEnd == null) throw new BusinessException("PARAM", "签退时间不能为空");
        Reservation reservation = require(resNo);
        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new BusinessException("STATE", "只有已签到的记录可以签退");
        }
        Date end = DateTimes.at(DateTimes.date(reservation.getStartTime()), actualEnd);
        if (end.before(reservation.getStartTime())) {
            throw new BusinessException("PARAM", "签退时间不能早于预约开始时间");
        }

        reservation.checkOut();
        reservationDao.update(reservation);
        setSeatStatus(reservation.getSeatNo(), SeatStatus.AVAILABLE);

        int penalty = 0;
        if (end.after(reservation.getEndTime())) {
            penalty = 1;
            deduct(reservation.getStudentId(), LATE_PENALTY, CreditChangeReason.LATE_CHECKOUT,
                    reservation.getReservationId());
        }

        CheckinRecord record = checkinRecordDao.findByResNo(resNo);
        if (record == null) {
            record = newRecord(resNo, reservation.getStartTime(), end, penalty);
            checkinRecordDao.save(record);
        } else {
            record.setActualEnd(end);
            record.setPenalty(penalty);
            checkinRecordDao.update(record);
        }
        return record;
    }

    @Override
    public int handleNoShow(LocalDateTime now) {
        if (now == null) throw new BusinessException("PARAM", "判定时间不能为空");
        Date when = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        int handled = 0;
        for (Reservation reservation : reservationDao.findAll()) {
            if (reservation.getStatus() != ReservationStatus.BOOKED) continue;
            Date deadline = reservation.getSignInDeadline() != null
                    ? reservation.getSignInDeadline()
                    : DateTimes.plusMinutes(reservation.getStartTime(), DEADLINE_MINUTES);
            if (!when.after(deadline)) continue;
            reservation.handleNoShow();
            reservationDao.update(reservation);
            release(reservation.getSeatNo(), SeatStatus.RESERVED);
            deduct(reservation.getStudentId(), NO_SHOW_PENALTY, CreditChangeReason.NO_SHOW,
                    reservation.getReservationId());
            handled++;
        }
        return handled;
    }

    /** 取预约记录并校验：空号抛 PARAM，查不到抛 NOT_FOUND。 */
    private Reservation require(String resNo) {
        if (resNo == null || resNo.isBlank()) throw new BusinessException("PARAM", "预约号不能为空");
        Reservation reservation = reservationDao.findByNo(resNo);
        if (reservation == null) throw new BusinessException("NOT_FOUND", "预约记录不存在：" + resNo);
        return reservation;
    }

    /** 按预约开始时间倒序，方便界面把最近的排在最前。 */
    private List<Reservation> sortByStartDesc(List<Reservation> reservations) {
        List<Reservation> sorted = new ArrayList<>(reservations);
        sorted.sort((a, b) -> {
            if (a.getStartTime() == null || b.getStartTime() == null) return 0;
            return b.getStartTime().compareTo(a.getStartTime());
        });
        return sorted;
    }

    /** 同一座位在 [from, to) 内是否存在占用中的预约。 */
    private boolean occupied(List<Reservation> reservations, String seatNo, Date from, Date to) {
        for (Reservation r : reservations) {
            if (r.getSeatNo() == null || !r.getSeatNo().equals(seatNo)) continue;
            if (r.getStatus() != ReservationStatus.BOOKED && r.getStatus() != ReservationStatus.CHECKED_IN) continue;
            if (r.getStartTime() == null || r.getEndTime() == null) continue;
            if (r.getStartTime().before(to) && r.getEndTime().after(from)) return true;
        }
        return false;
    }

    /** 释放座位；仅当座位处于预期状态时才改回 AVAILABLE，避免误改维修中的座位。 */
    private void release(String seatNo, SeatStatus expected) {
        Seat seat = seatDao.findByNo(seatNo);
        if (seat == null || seat.getStatus() != expected) return;
        seat.updateStatus(SeatStatus.AVAILABLE);
        seatDao.update(seat);
    }

    private void setSeatStatus(String seatNo, SeatStatus status) {
        Seat seat = seatDao.findByNo(seatNo);
        if (seat == null) throw new BusinessException("SEAT_NOT_FOUND", "座位不存在：" + seatNo);
        seat.updateStatus(status);
        seatDao.update(seat);
    }

    private void deduct(String studentId, int delta, CreditChangeReason reason, Long reservationId) {
        Student student = studentDao.findByStudentId(studentId);
        if (student == null) throw new BusinessException("STUDENT_NOT_FOUND", "学生档案不存在：" + studentId);
        int score = student.getCreditScore() == null ? 0 : student.getCreditScore();
        student.setCreditScore(score + delta);
        studentDao.update(student);
        creditScoreLogDao.save(new CreditScoreLog(null, studentId, reservationId, delta, reason, new Date()));
    }

    private CheckinRecord newRecord(String resNo, Date start, Date end, int penalty) {
        CheckinRecord record = new CheckinRecord(nextRecordNo(), start, end, penalty);
        record.setResNo(resNo);
        record.setCreateTime(new Date());
        return record;
    }

    private String nextRecordNo() {
        return "REC" + new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.ROOT).format(new Date());
    }
}
