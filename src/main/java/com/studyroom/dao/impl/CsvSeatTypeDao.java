package com.studyroom.dao.impl;

import com.studyroom.entity.SeatType;

import java.util.ArrayList;
import java.util.List;

/** 座位区域读取：seatCount 为派生值，实时按 seats.csv 统计。 */
public class CsvSeatTypeDao {
    private final CsvSeatDao seatDao;

    public CsvSeatTypeDao(CsvSeatDao seatDao) {
        this.seatDao = seatDao;
    }

    public List<SeatType> findAll() {
        List<SeatType> types = new ArrayList<>();
        for (var seat : seatDao.findAll()) {
            String name = seat.getRoomName();
            if (name == null || types.stream().anyMatch(t -> name.equals(t.getTypeName()))) continue;
            types.add(new SeatType(name, seatDao.countByTypeName(name)));
        }
        return types;
    }

    public SeatType findByTypeName(String typeName) {
        return findAll().stream().filter(t -> typeName != null && typeName.equals(t.getTypeName()))
                .findFirst().orElse(null);
    }
}
