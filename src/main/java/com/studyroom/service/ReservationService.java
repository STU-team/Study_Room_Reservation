package com.studyroom.service;

import com.studyroom.entity.CheckinRecord;
import com.studyroom.entity.Reservation;
import com.studyroom.entity.Seat;
import com.studyroom.entity.Student;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** 预约业务入口（契约第六节）。 */
public interface ReservationService {

    /** 查询指定日期与时段内某区域的可用座位，typeName 传 null 表示不限区域。 */
    List<Seat> listAvailableSeats(LocalDate date, LocalTime start, LocalTime end, String typeName);

    /** 在线预约：信用分 >= 80、座位可用且时段不冲突时生成预约记录。 */
    Reservation reserve(String seatNo, Student student, LocalDate date, LocalTime start, LocalTime end)
            throws BusinessException;

    /** 取消预约：仅允许签到前取消 BOOKED 状态的预约，并释放座位。 */
    Reservation cancelReservation(String resNo) throws BusinessException;

    /** 签到登记：BOOKED -> CHECKED_IN，座位 -> OCCUPIED。 */
    Reservation signIn(String resNo) throws BusinessException;

    /** 签退释放：释放座位，实际结束晚于预约结束时扣 1 分，返回签退记录。 */
    CheckinRecord signOut(String resNo, LocalTime actualEnd) throws BusinessException;

    /** 爽约处理：扫描已过签到截止时间的 BOOKED 预约，置 NO_SHOW、释放座位、扣 5 分并写流水。 */
    int handleNoShow(LocalDateTime now) throws BusinessException;
}
