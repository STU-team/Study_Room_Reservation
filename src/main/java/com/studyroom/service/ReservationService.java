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

    /** 查询某个学生的全部预约记录，按预约开始时间倒序；studentId 为空时抛 PARAM。 */
    List<Reservation> listReservations(String studentId);

    /** 查询全部预约记录，按预约开始时间倒序，供管理员界面展示。 */
    List<Reservation> listAllReservations();

    /** 查询当前在座记录（并已签到未签退，即 actualEnd 为空的签到记录）。 */
    List<CheckinRecord> listSeated();

    /** 按预约号查单条预约，供在座名单等场景补座位与预约人信息；不存在抛 NOT_FOUND。 */
    Reservation findReservation(String resNo) throws BusinessException;

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

    /**
     * 系统管理员把座位转维修的编排入口（v1.3）：取消该座位全部 BOOKED 预约
     * （已过截止的按爽约扣 5 分置 NO_SHOW，未过的置 CANCELLED），最后座位置 CLOSED。
     * 返回处理掉的预约条数。
     */
    int closeSeatForRepair(String seatNo) throws BusinessException;
}
