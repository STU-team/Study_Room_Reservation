package com.studyroom.dao;

import com.studyroom.entity.CheckinRecord;

/** 签到签退记录数据访问接口（方法签名遵循接口契约 v0.6）。 */
public interface CheckinRecordDao {
    void save(CheckinRecord record);
    CheckinRecord findByResNo(String resNo);
}
