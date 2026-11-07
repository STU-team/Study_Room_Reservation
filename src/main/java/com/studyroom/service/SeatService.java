package com.studyroom.service;

import com.studyroom.entity.Seat;
import com.studyroom.entity.SeatStatus;

import java.util.List;

/** 座位业务入口（契约第六节）。 */
public interface SeatService {

    List<Seat> listAllSeats();

    Seat findByNo(String seatNo) throws BusinessException;

    /** 更新座位状态；OCCUPIED 不得直接改为 AVAILABLE，须先办签退。 */
    void updateSeatStatus(String seatNo, SeatStatus status) throws BusinessException;
}
