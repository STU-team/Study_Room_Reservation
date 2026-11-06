package com.studyroom.dao.impl;

import com.studyroom.dao.SeatTypeDao;
import com.studyroom.entity.SeatType;

import java.util.ArrayList;
import java.util.List;

/** 座位区域读取（契约 v0.7 第七节 SeatTypeDao 实现）：seatCount 实时按 seats.csv 统计。 */
public class CsvSeatTypeDao implements SeatTypeDao {
    private final CsvSeatDao seatDao;

    public CsvSeatTypeDao(CsvSeatDao seatDao) {
        this.seatDao = seatDao;
    }

    @Override
    public List<SeatType> findAll() {
        List<SeatType> types = new ArrayList<>();
        for (var seat : seatDao.findAll()) {
            String name = seat.getRoomName();
            if (name == null || types.stream().anyMatch(t -> name.equals(t.getTypeName()))) continue;
            types.add(new SeatType(name, seatDao.countByTypeName(name)));
        }
        return types;
    }

    @Override
    public SeatType findByTypeName(String typeName) {
        return findAll().stream().filter(t -> typeName != null && typeName.equals(t.getTypeName()))
                .findFirst().orElse(null);
    }
}
