package com.studyroom.entity;

/** 具体座位。状态流转的业务校验由 A-6 的 SeatService 负责。 */
public class Seat {
    private Long seatId;
    private String roomName;
    private String seatNo;
    private SeatStatus status;

    public Seat() {}

    public Seat(Long seatId, String roomName, String seatNo, SeatStatus status) {
        this.seatId = seatId;
        this.roomName = roomName;
        this.seatNo = seatNo;
        this.status = status;
    }

    public Long getSeatId() { return seatId; }
    public void setSeatId(Long seatId) { this.seatId = seatId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getSeatNo() { return seatNo; }
    public void setSeatNo(String seatNo) { this.seatNo = seatNo; }
    public SeatStatus getStatus() { return status; }
    public void setStatus(SeatStatus status) { this.status = status; }

    /** 类图中的状态更新操作；是否允许该业务转移由 Service 层按状态图校验。 */
    public void updateStatus(SeatStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("座位状态不能为空");
        }
        this.status = newStatus;
    }
}
