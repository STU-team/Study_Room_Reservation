package com.studyroom.entity;

import java.util.Date;

/** 信用分变动流水；changeValue 扣分为负数，加分为正数。 */
public class CreditScoreLog {
    private Long logId;
    private String studentId;
    private Long reservationId;
    private Integer changeValue;
    private CreditChangeReason reason;
    private Date createTime;

    public CreditScoreLog() {}

    public CreditScoreLog(Long logId, String studentId, Long reservationId, Integer changeValue,
                          CreditChangeReason reason, Date createTime) {
        this.logId = logId;
        this.studentId = studentId;
        this.reservationId = reservationId;
        this.changeValue = changeValue;
        this.reason = reason;
        this.createTime = copy(createTime);
    }

    private static Date copy(Date value) { return value == null ? null : new Date(value.getTime()); }

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public Integer getChangeValue() { return changeValue; }
    public void setChangeValue(Integer changeValue) { this.changeValue = changeValue; }
    public CreditChangeReason getReason() { return reason; }
    public void setReason(CreditChangeReason reason) { this.reason = reason; }
    public Date getCreateTime() { return copy(createTime); }
    public void setCreateTime(Date createTime) { this.createTime = copy(createTime); }
}
