package com.studyroom.dao;

import com.studyroom.entity.SeatType;

import java.util.List;

/** 座位区域读取（契约 v0.7 第七节）；seatCount 为派生值，由 Seat 统计。 */
public interface SeatTypeDao {
    List<SeatType> findAll();

    SeatType findByTypeName(String typeName);
}
