package com.studyroom.dao;

import com.studyroom.entity.CheckinRecord;

import java.util.List;

/** 签到签退记录数据访问接口（方法签名遵循接口契约 v1.1）。 */
public interface CheckinRecordDao {
    void save(CheckinRecord record);
    void update(CheckinRecord record);
    CheckinRecord findByResNo(String resNo);
    List<CheckinRecord> findAll();
}
