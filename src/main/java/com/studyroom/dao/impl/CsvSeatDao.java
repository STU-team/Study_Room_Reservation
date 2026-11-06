package com.studyroom.dao.impl;

import com.studyroom.dao.DataAccessException;
import com.studyroom.dao.SeatDao;
import com.studyroom.entity.Seat;
import com.studyroom.entity.SeatStatus;
import com.studyroom.util.CsvMapper;
import com.studyroom.util.CsvStore;
import com.studyroom.util.DataInitializer;

import java.nio.file.Path;
import java.util.List;

/** SeatDao 的 CSV 实现：seatId 由 seatNo 散列，roomName 复用 typeName 列。 */
public class CsvSeatDao implements SeatDao {
    private static final List<String> HEADER = DataInitializer.headers().get("seats.csv");
    private final CsvStore store;

    public CsvSeatDao(Path dataDirectory) {
        this.store = new CsvStore(dataDirectory.resolve("seats.csv"), HEADER);
    }

    @Override
    public synchronized List<Seat> findAll() {
        return store.data().stream()
                .map(r -> new Seat(CsvMapper.id(r.get(0)), r.get(1), r.get(0), SeatStatus.valueOf(r.get(2))))
                .toList();
    }

    @Override
    public synchronized Seat findByNo(String seatNo) {
        return seatNo == null ? null
                : findAll().stream().filter(s -> seatNo.equals(s.getSeatNo())).findFirst().orElse(null);
    }

    @Override
    public synchronized void update(Seat seat) {
        List<List<String>> rows = store.all();
        int i = store.indexOf(rows, 0, seat.getSeatNo());
        if (i < 0) throw new DataAccessException("座位不存在：" + seat.getSeatNo());
        String type = seat.getRoomName() == null || seat.getRoomName().isBlank() ? rows.get(i).get(1) : seat.getRoomName();
        rows.set(i, List.of(seat.getSeatNo(), type, seat.getStatus().name()));
        store.save(rows);
    }

    /** 区域座位数（SeatType.seatCount 的派生来源）。 */
    public synchronized int countByTypeName(String typeName) {
        return (int) store.data().stream().filter(r -> typeName != null && typeName.equals(r.get(1))).count();
    }
}
