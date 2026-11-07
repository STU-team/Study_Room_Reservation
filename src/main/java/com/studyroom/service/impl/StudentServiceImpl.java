package com.studyroom.service.impl;

import com.studyroom.dao.StudentDao;
import com.studyroom.entity.Student;
import com.studyroom.service.BusinessException;
import com.studyroom.service.StudentService;

import java.util.ArrayList;
import java.util.List;

/** 学生档案实现：只读，不改信用分（改动统一由预约业务按规则落账）。 */
public class StudentServiceImpl implements StudentService {
    private final StudentDao studentDao;

    public StudentServiceImpl(StudentDao studentDao) {
        this.studentDao = studentDao;
    }

    @Override
    public Student findByStudentId(String studentId) {
        if (studentId == null || studentId.isBlank()) throw new BusinessException("PARAM", "学号不能为空");
        Student student = studentDao.findByStudentId(studentId);
        if (student == null) throw new BusinessException("STUDENT_NOT_FOUND", "学生档案不存在：" + studentId);
        return student;
    }

    @Override
    public List<Student> listStudents() {
        List<Student> students = new ArrayList<>(studentDao.findAll());
        students.sort((a, b) -> Integer.compare(a.getCreditScore(), b.getCreditScore()));
        return students;
    }
}
