package com.studyroom.dao;

import com.studyroom.entity.CreditScoreLog;

import java.util.List;

/** 信用分变动流水读写（契约 v0.7 第七节）。 */
public interface CreditScoreLogDao {
    void save(CreditScoreLog log);

    List<CreditScoreLog> findByStudentId(String studentId);

    List<CreditScoreLog> findAll();
}
