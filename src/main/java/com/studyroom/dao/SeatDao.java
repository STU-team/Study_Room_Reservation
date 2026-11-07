package com.studyroom.dao;

import com.studyroom.entity.Seat;

import java.util.List;

/** 座位数据访问接口（方法签名遵循接口契约 v0.6）。 */
public interface SeatDao {
    List<Seat> findAll();
    Seat findByNo(String seatNo);
    void update(Seat seat);
}
