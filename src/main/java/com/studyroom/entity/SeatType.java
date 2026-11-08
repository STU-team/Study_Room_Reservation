package com.studyroom.entity;

/** 座位类型。seatCount 是派生值，由 Seat 按 typeName 统计，不独立持久化。 */
public class SeatType {
    private String typeName;
    private Integer seatCount;

    public SeatType() {}

    public SeatType(String typeName, Integer seatCount) {
        this.typeName = typeName;
        this.seatCount = seatCount;
    }

    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public Integer getSeatCount() { return seatCount; }
    public void setSeatCount(Integer seatCount) { this.seatCount = seatCount; }
}
