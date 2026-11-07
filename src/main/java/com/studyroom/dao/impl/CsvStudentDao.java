package com.studyroom.dao.impl;

import com.studyroom.dao.DataAccessException;
import com.studyroom.dao.StudentDao;
import com.studyroom.entity.Student;
import com.studyroom.util.CsvMapper;
import com.studyroom.util.CsvStore;
import com.studyroom.util.DataInitializer;

import java.nio.file.Path;
import java.util.List;

/** StudentDao 的 CSV 实现：表头与实体一一对应，update 不做隐式新增。 */
public class CsvStudentDao implements StudentDao {
    private static final List<String> HEADER = DataInitializer.headers().get("students.csv");
    private final CsvStore store;

    public CsvStudentDao(Path dataDirectory) {
        this.store = new CsvStore(dataDirectory.resolve("students.csv"), HEADER);
    }

    @Override
    public synchronized Student findByStudentId(String studentId) {
        return studentId == null || studentId.isBlank() ? null
                : findAll().stream().filter(s -> studentId.equals(s.getStudentId())).findFirst().orElse(null);
    }

    @Override
    public synchronized List<Student> findAll() {
        return store.data().stream()
                .map(r -> new Student(r.get(1), r.get(0), r.get(2), CsvMapper.integer(r.get(3))))
                .toList();
    }

    @Override
    public synchronized void update(Student student) {
        List<List<String>> rows = store.all();
        int i = store.indexOf(rows, 1, student.getStudentId());
        if (i < 0) throw new DataAccessException("待更新的学生不存在：" + student.getStudentId());
        rows.set(i, List.of(student.getName(), student.getStudentId(),
                CsvMapper.text(student.getPhone()), String.valueOf(student.getCreditScore())));
        store.save(rows);
    }
}
