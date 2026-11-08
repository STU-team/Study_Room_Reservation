package com.studyroom.dao;

import com.studyroom.entity.Reservation;

import java.util.List;

/** 预约记录数据访问接口（方法签名遵循接口契约 v0.6）。 */
public interface ReservationDao {
    void save(Reservation reservation);
    Reservation findByNo(String resNo);
    List<Reservation> findAll();
    void update(Reservation reservation);
    String nextResNo();
}
