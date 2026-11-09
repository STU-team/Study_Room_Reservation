package com.studyroom.service.impl;

import com.studyroom.dao.SeatDao;
import com.studyroom.entity.Seat;
import com.studyroom.entity.SeatStatus;
import com.studyroom.service.BusinessException;
import com.studyroom.service.SeatService;

import java.util.List;

/** 座位业务实现：手动改状态按 v1.3 矩阵收紧，不承担业务流程。 */
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
        SeatStatus from = seat.getStatus();
        if (from == status) return;
        if (from == SeatStatus.AVAILABLE && status == SeatStatus.CLOSED) {
            seat.updateStatus(status); seatDao.update(seat); return;
        }
        if (from == SeatStatus.CLOSED && status == SeatStatus.AVAILABLE) {
            seat.updateStatus(status); seatDao.update(seat); return;
        }
        if (from == SeatStatus.OCCUPIED) {
            throw new BusinessException("SEAT_IN_USE", "座位使用中，须先办理签退");
        }
        if (from == SeatStatus.RESERVED) {
            throw new BusinessException("STATE", "座位已被预约，不能手动改为「" + cn(status)
                    + "」；请先取消该座位全部预约或等待签到/爽约处理");
        }
        throw new BusinessException("STATE", "不支持从「" + cn(from) + "」直接改为「" + cn(status) + "」");
    }

    private static String cn(SeatStatus s) {
        return switch (s) {
            case AVAILABLE -> "可用";
            case RESERVED -> "已预约";
            case OCCUPIED -> "使用中";
            case CLOSED -> "维修";
        };
    }
}
