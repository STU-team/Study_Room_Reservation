package com.studyroom.entity;

import java.util.Date;

/**
 * 一次签到/签退记录。记录写入后不再改变业务状态。
 *
 * <p>resNo 与 createTime 来自契约 v0.6 第七节 {@code findByResNo} 查询与第八节 CSV 表头，
 * A-3 类图未画这两个属性，但持久化与查询必需，故在实体中补充。</p>
 */
public class CheckinRecord {
    private String recordNo;
    private String resNo;
    private Date actualStart;
    private Date actualEnd;
    private Integer penalty;
    private Date createTime;

    public CheckinRecord() {}

    public CheckinRecord(String recordNo, Date actualStart, Date actualEnd, Integer penalty) {
        this.recordNo = recordNo;
        this.actualStart = copy(actualStart);
        this.actualEnd = copy(actualEnd);
        this.penalty = penalty;
    }

    private static Date copy(Date value) { return value == null ? null : new Date(value.getTime()); }

    public String getRecordNo() { return recordNo; }
    public void setRecordNo(String recordNo) { this.recordNo = recordNo; }
    public String getResNo() { return resNo; }
    public void setResNo(String resNo) { this.resNo = resNo; }
    public Date getActualStart() { return copy(actualStart); }
    public void setActualStart(Date actualStart) { this.actualStart = copy(actualStart); }
    public Date getActualEnd() { return copy(actualEnd); }
    public void setActualEnd(Date actualEnd) { this.actualEnd = copy(actualEnd); }
    public Integer getPenalty() { return penalty; }
    public void setPenalty(Integer penalty) { this.penalty = penalty; }
    public Date getCreateTime() { return copy(createTime); }
    public void setCreateTime(Date createTime) { this.createTime = copy(createTime); }
}
