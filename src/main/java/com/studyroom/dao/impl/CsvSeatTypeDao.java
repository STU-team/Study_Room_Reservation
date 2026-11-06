package com.studyroom.dao.impl;

import com.studyroom.entity.SeatType;

import java.util.ArrayList;
import java.util.List;

/**
 * 座位区域读取（契约 v0.6 第八节 seat_types.csv：typeName,seatCount）。
 *
 * <p>契约第七节尚未声明 SeatTypeDao 接口，故先以独立实现类提供读取能力。
 * seatCount 是 A-3 类图中的派生属性（{derived}），权威值应为该区域下 Seat 记录数，
 * 因此读取时以 {@link CsvSeatDao#countByTypeName(String)} 统计结果为准，CSV 中的
 * seatCount 列只作冗余展示，不参与业务判断。</p>
 */
public class CsvSeatTypeDao {
    private final CsvSeatDao seatDao;

    public CsvSeatTypeDao(CsvSeatDao seatDao) {
        this.seatDao = java.util.Objects.requireNonNull(seatDao, "seatDao 不能为空");
    }

    /** 按 seats.csv 中出现的区域名去重列出座位区域，seatCount 为实时统计值。 */
    public List<SeatType> findAll() {
        List<SeatType> types = new ArrayList<>();
        for (String typeName : distinctTypeNames()) {
            types.add(new SeatType(typeName, seatDao.countByTypeName(typeName)));
        }
        return types;
    }

    public SeatType findByTypeName(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            return null;
        }
        for (SeatType type : findAll()) {
            if (typeName.equals(type.getTypeName())) {
                return type;
            }
        }
        return null;
    }

    private List<String> distinctTypeNames() {
        List<String> names = new ArrayList<>();
        for (var seat : seatDao.findAll()) {
            String roomName = seat.getRoomName();
            if (roomName != null && !roomName.isBlank() && !names.contains(roomName)) {
                names.add(roomName);
            }
        }
        return names;
    }
}
