package com.studyroom.service;

import com.studyroom.entity.Student;

import java.util.List;

/** 学生档案查询入口（契约第六节，v1.2 新增）：界面用它取信用分与姓名。 */
public interface StudentService {

    /** 按学号查学生档案；不存在抛 STUDENT_NOT_FOUND。 */
    Student findByStudentId(String studentId) throws BusinessException;

    /** 全部学生档案，按信用分升序，供管理端查看谁需要核销扣分。 */
    List<Student> listStudents();
}
