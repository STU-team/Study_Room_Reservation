package com.studyroom.service;

import com.studyroom.entity.Seat;
import com.studyroom.entity.SeatStatus;

import java.util.List;

/** 座位业务入口（契约第六节）。 */
public interface SeatService {

    List<Seat> listAllSeats();

    Seat findByNo(String seatNo) throws BusinessException;

    /**
     * 手动维护座位状态（v1.3 矩阵）：
     * AVAILABLE↔CLOSED 允许；OCCUPIED / RESERVED 不得手动改为任何状态。
     * 要把 OCCUPIED / RESERVED 的座位转维修，请走 {@link ReservationService#closeSeatForRepair}。
     */
    void updateSeatStatus(String seatNo, SeatStatus status) throws BusinessException;
}
