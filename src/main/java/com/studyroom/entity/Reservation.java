package com.studyroom.entity;

import java.util.Date;

/** 预约记录。时间字段按 A-3 最终类图使用完整日期时间（java.util.Date）。
 * resNo 是契约 v0.6 中面向界面的业务预约号（CSV 主键），与 Long 型 reservationId 并存。 */
public class Reservation {
    private String resNo;
    private String studentId;
    private String seatNo;
    private Long reservationId;
    private Long userId;
    private Long seatId;
    private Date createTime;
    private Date startTime;
    private Date endTime;
    private Date signInDeadline;
    private ReservationStatus status;

    public Reservation() {}

    public Reservation(Long reservationId, Long userId, Long seatId, Date createTime,
                       Date startTime, Date endTime, Date signInDeadline, ReservationStatus status) {
        this.reservationId = reservationId;
        this.userId = userId;
        this.seatId = seatId;
        this.createTime = copy(createTime);
        this.startTime = copy(startTime);
        this.endTime = copy(endTime);
        this.signInDeadline = copy(signInDeadline);
        this.status = status;
    }

    private static Date copy(Date value) { return value == null ? null : new Date(value.getTime()); }

    public String getResNo() { return resNo; }
    public void setResNo(String resNo) { this.resNo = resNo; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getSeatNo() { return seatNo; }
    public void setSeatNo(String seatNo) { this.seatNo = seatNo; }
    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSeatId() { return seatId; }
    public void setSeatId(Long seatId) { this.seatId = seatId; }
    public Date getCreateTime() { return copy(createTime); }
    public void setCreateTime(Date createTime) { this.createTime = copy(createTime); }
    public Date getStartTime() { return copy(startTime); }
    public void setStartTime(Date startTime) { this.startTime = copy(startTime); }
    public Date getEndTime() { return copy(endTime); }
    public void setEndTime(Date endTime) { this.endTime = copy(endTime); }
    public Date getSignInDeadline() { return copy(signInDeadline); }
    public void setSignInDeadline(Date signInDeadline) { this.signInDeadline = copy(signInDeadline); }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }

    /** 预约人主动取消；只负责状态迁移，座位释放由 Service 编排。 */
    public boolean cancel() {
        if (status != ReservationStatus.BOOKED) return false;
        status = ReservationStatus.CANCELLED;
        return true;
    }

    /** 值班管理员签到；时间核验与座位更新由 Service 编排。 */
    public boolean checkIn() {
        if (status != ReservationStatus.BOOKED) return false;
        status = ReservationStatus.CHECKED_IN;
        return true;
    }

    /** 值班管理员签退；扣分与记录写入由 Service 编排。 */
    public boolean checkOut() {
        if (status != ReservationStatus.CHECKED_IN) return false;
        status = ReservationStatus.CHECKED_OUT;
        return true;
    }

    /** 后台爽约判定；扣分、流水写入与座位释放由 Service 编排。 */
    public void handleNoShow() {
        if (status != ReservationStatus.BOOKED) {
            throw new IllegalStateException("只有 BOOKED 状态的预约才能判定为 NO_SHOW");
        }
        status = ReservationStatus.NO_SHOW;
    }
}
