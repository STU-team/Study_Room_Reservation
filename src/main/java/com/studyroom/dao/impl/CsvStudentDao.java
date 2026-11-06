package com.studyroom.dao.impl;

import com.studyroom.dao.DataAccessException;
import com.studyroom.dao.StudentDao;
import com.studyroom.entity.Student;
import com.studyroom.util.CsvUtil;
import com.studyroom.util.DataInitializer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * StudentDao 的 CSV 实现。students.csv 的字段与 A-3 Student 实体一一对应，
 * 因此可完整保存并恢复学生档案。
 */
public class CsvStudentDao implements StudentDao {
    private static final List<String> HEADER = DataInitializer.headers().get("students.csv");
    private final Path file;

    public CsvStudentDao(Path dataDirectory) {
        Objects.requireNonNull(dataDirectory, "dataDirectory 不能为空");
        this.file = dataDirectory.resolve("students.csv");
        try {
            CsvUtil.ensureHeader(file, HEADER);
        } catch (IOException ex) {
            throw new DataAccessException("初始化 students.csv 失败", ex);
        }
    }

    @Override
    public synchronized Student findByStudentId(String studentId) {
        if (studentId == null || studentId.isBlank()) return null;
        return findAll().stream()
                .filter(student -> studentId.equals(student.getStudentId()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public synchronized void update(Student student) {
        validate(student);
        List<List<String>> rows = readRows();
        int foundIndex = -1;
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (row.size() != HEADER.size()) {
                throw new DataAccessException("students.csv 第 " + (i + 1) + " 行字段数量错误");
            }
            if (student.getStudentId().equals(row.get(1))) {
                if (foundIndex >= 0) {
                    throw new DataAccessException("students.csv 中存在重复学号：" + student.getStudentId());
                }
                foundIndex = i;
            }
        }
        if (foundIndex < 0) {
            throw new DataAccessException("待更新的学生不存在：" + student.getStudentId());
        }
        rows.set(foundIndex, toRow(student));
        writeRows(rows);
    }

    @Override
    public synchronized List<Student> findAll() {
        List<List<String>> rows = readRows();
        List<Student> students = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (row.size() != HEADER.size()) {
                throw new DataAccessException("students.csv 第 " + (i + 1) + " 行字段数量错误");
            }
            try {
                int creditScore = Integer.parseInt(row.get(3));
                students.add(new Student(row.get(1), row.get(0), row.get(2), creditScore));
            } catch (NumberFormatException ex) {
                throw new DataAccessException("students.csv 第 " + (i + 1) + " 行信用分不是整数", ex);
            }
        }
        return students;
    }

    private List<List<String>> readRows() {
        try {
            List<List<String>> rows = CsvUtil.readAll(file);
            if (rows.isEmpty() || !HEADER.equals(rows.get(0))) {
                throw new DataAccessException("students.csv 表头缺失或与契约不一致");
            }
            return rows;
        } catch (IOException ex) {
            throw new DataAccessException("读取 students.csv 失败", ex);
        }
    }

    private void writeRows(List<List<String>> rows) {
        try {
            CsvUtil.writeAll(file, rows);
        } catch (IOException ex) {
            throw new DataAccessException("写入 students.csv 失败", ex);
        }
    }

    private static List<String> toRow(Student student) {
        return List.of(student.getName(), student.getStudentId(),
                Objects.toString(student.getPhone(), ""), String.valueOf(student.getCreditScore()));
    }

    private static void validate(Student student) {
        Objects.requireNonNull(student, "student 不能为空");
        if (student.getStudentId() == null || student.getStudentId().isBlank()) {
            throw new IllegalArgumentException("studentId 不能为空");
        }
        if (student.getName() == null || student.getName().isBlank()) {
            throw new IllegalArgumentException("学生姓名不能为空");
        }
        if (student.getCreditScore() == null) {
            throw new IllegalArgumentException("creditScore 不能为空");
        }
    }
}
