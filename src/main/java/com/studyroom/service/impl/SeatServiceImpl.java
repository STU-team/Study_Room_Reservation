package com.studyroom.service.impl;

import com.studyroom.dao.SeatDao;
import com.studyroom.entity.Seat;
import com.studyroom.entity.SeatStatus;
import com.studyroom.service.BusinessException;
import com.studyroom.service.SeatService;

import java.util.List;

/** 座位业务实现：OCCUPIED 不得直接转 AVAILABLE（契约 v0.5 约束、A-4 状态图备注）。 */
public class SeatServiceImpl implements SeatService {
    private final SeatDao seatDao;

    public SeatServiceImpl(SeatDao seatDao) {
        this.seatDao = seatDao;
    }

    @Override
    public List<Seat> listAllSeats() {
        return seatDao.findAll();
    }

    @Override
    public Seat findByNo(String seatNo) {
        if (seatNo == null || seatNo.isBlank()) throw new BusinessException("PARAM", "座位号不能为空");
        Seat seat = seatDao.findByNo(seatNo);
        if (seat == null) throw new BusinessException("SEAT_NOT_FOUND", "座位不存在：" + seatNo);
        return seat;
    }

    @Override
    public void updateSeatStatus(String seatNo, SeatStatus status) {
        if (status == null) throw new BusinessException("PARAM", "座位状态不能为空");
        Seat seat = findByNo(seatNo);
        if (seat.getStatus() == SeatStatus.OCCUPIED && status == SeatStatus.AVAILABLE) {
            throw new BusinessException("SEAT_IN_USE", "座位使用中，须先办理签退");
        }
        seat.updateStatus(status);
        seatDao.update(seat);
    }
}
