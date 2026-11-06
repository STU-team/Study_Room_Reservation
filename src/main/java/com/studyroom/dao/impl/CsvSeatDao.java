package com.studyroom.dao.impl;

import com.studyroom.dao.DataAccessException;
import com.studyroom.dao.SeatDao;
import com.studyroom.entity.Seat;
import com.studyroom.entity.SeatStatus;
import com.studyroom.util.CsvMapper;
import com.studyroom.util.CsvUtil;
import com.studyroom.util.DataInitializer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SeatDao 的 CSV 实现。
 *
 * <p>契约 v0.6 的 seats.csv 只有 seatNo、typeName、status 三列，没有 seatId 与 roomName。
 * 映射方式：seatId 取 seatNo 的稳定散列（同一座位号恒等，重启后可还原）；roomName 与
 * typeName 同义，读入时 roomName 取 typeName，写回时 typeName 取 roomName。</p>
 */
public class CsvSeatDao implements SeatDao {
    private static final List<String> HEADER = DataInitializer.headers().get("seats.csv");
    private static final int COL_SEAT_NO = 0;
    private static final int COL_TYPE_NAME = 1;
    private static final int COL_STATUS = 2;

    private final Path file;

    public CsvSeatDao(Path dataDirectory) {
        Objects.requireNonNull(dataDirectory, "dataDirectory 不能为空");
        this.file = dataDirectory.resolve("seats.csv");
        try {
            CsvUtil.ensureHeader(file, HEADER);
        } catch (IOException ex) {
            throw new DataAccessException("初始化 seats.csv 失败", ex);
        }
    }

    @Override
    public synchronized List<Seat> findAll() {
        List<List<String>> rows = readRows();
        List<Seat> seats = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            seats.add(toSeat(rows.get(i), i));
        }
        return seats;
    }

    @Override
    public synchronized Seat findByNo(String seatNo) {
        if (seatNo == null || seatNo.isBlank()) {
            return null;
        }
        return findAll().stream()
                .filter(seat -> seatNo.equals(seat.getSeatNo()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public synchronized void update(Seat seat) {
        Objects.requireNonNull(seat, "seat 不能为空");
        if (seat.getSeatNo() == null || seat.getSeatNo().isBlank()) {
            throw new IllegalArgumentException("seatNo 不能为空");
        }
        if (seat.getStatus() == null) {
            throw new IllegalArgumentException("座位状态不能为空");
        }
        List<List<String>> rows = readRows();
        int found = -1;
        for (int i = 1; i < rows.size(); i++) {
            if (seat.getSeatNo().equals(rows.get(i).get(COL_SEAT_NO))) {
                if (found >= 0) {
                    throw new DataAccessException("seats.csv 中存在重复座位号：" + seat.getSeatNo());
                }
                found = i;
            }
        }
        if (found < 0) {
            throw new DataAccessException("待更新的座位不存在：" + seat.getSeatNo());
        }
        List<String> row = rows.get(found);
        String typeName = seat.getRoomName() != null && !seat.getRoomName().isBlank()
                ? seat.getRoomName() : row.get(COL_TYPE_NAME);
        rows.set(found, List.of(seat.getSeatNo(), typeName, seat.getStatus().name()));
        writeRows(rows);
    }

    /** 供 SeatType 派生 seatCount 时复用：按区域统计座位数。 */
    public synchronized int countByTypeName(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            return 0;
        }
        List<List<String>> rows = readRows();
        int count = 0;
        for (int i = 1; i < rows.size(); i++) {
            if (typeName.equals(rows.get(i).get(COL_TYPE_NAME))) {
                count++;
            }
        }
        return count;
    }

    private Seat toSeat(List<String> row, int rowIndex) {
        requireWidth(row, rowIndex);
        String seatNo = row.get(COL_SEAT_NO);
        SeatStatus status;
        try {
            status = SeatStatus.valueOf(row.get(COL_STATUS).trim());
        } catch (IllegalArgumentException ex) {
            throw new DataAccessException("seats.csv 第 " + (rowIndex + 1) + " 行座位状态非法：" + row.get(COL_STATUS), ex);
        }
        return new Seat(CsvMapper.stableId(seatNo), row.get(COL_TYPE_NAME), seatNo, status);
    }

    private void requireWidth(List<String> row, int rowIndex) {
        if (row.size() != HEADER.size()) {
            throw new DataAccessException("seats.csv 第 " + (rowIndex + 1) + " 行字段数量错误");
        }
    }

    private List<List<String>> readRows() {
        try {
            List<List<String>> rows = CsvUtil.readAll(file);
            if (rows.isEmpty() || !HEADER.equals(rows.get(0))) {
                throw new DataAccessException("seats.csv 表头缺失或与契约不一致");
            }
            return rows;
        } catch (IOException ex) {
            throw new DataAccessException("读取 seats.csv 失败", ex);
        }
    }

    private void writeRows(List<List<String>> rows) {
        try {
            CsvUtil.writeAll(file, rows);
        } catch (IOException ex) {
            throw new DataAccessException("写入 seats.csv 失败", ex);
        }
    }
}
