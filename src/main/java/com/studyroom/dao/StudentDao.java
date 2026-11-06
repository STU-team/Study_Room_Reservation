package com.studyroom.dao;

import com.studyroom.entity.Student;

import java.util.List;

/** 学生档案数据访问接口（方法签名遵循接口契约 v0.6）。 */
public interface StudentDao {
    Student findByStudentId(String studentId);
    void update(Student student);
    List<Student> findAll();
}
