package com.studyroom.dao.impl;

import com.studyroom.dao.SeatTypeDao;
import com.studyroom.entity.Seat;
import com.studyroom.entity.SeatType;
import com.studyroom.util.CsvStore;
import com.studyroom.util.DataInitializer;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 座位区域（契约 v0.7 SeatTypeDao）：区域清单取 seat_types.csv，seatCount 按 seats.csv 实时统计。 */
public class CsvSeatTypeDao implements SeatTypeDao {
    private static final List<String> HEADER = DataInitializer.headers().get("seat_types.csv");
    private final CsvStore store;
    private final CsvSeatDao seatDao;

    public CsvSeatTypeDao(Path dataDirectory, CsvSeatDao seatDao) {
        this.store = new CsvStore(dataDirectory.resolve("seat_types.csv"), HEADER);
        this.seatDao = seatDao;
    }

    @Override
    public List<SeatType> findAll() {
        Set<String> names = new LinkedHashSet<>();
        for (List<String> row : store.data()) names.add(row.get(0));
        List<Seat> seats = seatDao.findAll();
        Map<String, Integer> counts = new HashMap<>();
        for (Seat seat : seats) {
            String name = seat.getRoomName();
            if (name == null) continue;
            counts.merge(name, 1, Integer::sum);
            names.add(name);
        }
        return names.stream().map(n -> new SeatType(n, counts.getOrDefault(n, 0))).toList();
    }

    @Override
    public SeatType findByTypeName(String typeName) {
        if (typeName == null) return null;
        return findAll().stream().filter(t -> typeName.equals(t.getTypeName())).findFirst().orElse(null);
    }
}
