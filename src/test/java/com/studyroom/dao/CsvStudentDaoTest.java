package com.studyroom.dao;

import com.studyroom.dao.impl.CsvStudentDao;
import com.studyroom.entity.Student;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvStudentDaoTest {
    @TempDir
    Path tempDir;

    @Test
    void newStudentFileStartsEmptyAndLookupReturnsNull() {
        CsvStudentDao dao = new CsvStudentDao(tempDir);
        assertTrue(dao.findAll().isEmpty());
        assertNull(dao.findByStudentId("20240001"));
    }

    @Test
    void updateOnlyUpdatesExistingStudentAndPreservesChineseAndCommaFields() throws Exception {
        CsvStudentDao dao = new CsvStudentDao(tempDir);
        Path csv = tempDir.resolve("students.csv");
        com.studyroom.util.CsvUtil.appendRow(csv,
                List.of("张三,同学", "20240001", "13800000000", "100"));

        Student loaded = dao.findByStudentId("20240001");
        assertNotNull(loaded);
        assertEquals("张三,同学", loaded.getName());
        assertEquals(100, loaded.getCreditScore());

        dao.update(new Student("20240001", "张三", "13800000000", 95));
        assertEquals(95, dao.findByStudentId("20240001").getCreditScore());
        assertEquals(1, dao.findAll().size());
    }

    @Test
    void updateMissingStudentFailsClearly() {
        CsvStudentDao dao = new CsvStudentDao(tempDir);
        assertThrows(DataAccessException.class,
                () -> dao.update(new Student("missing", "不存在", "", 100)));
    }
}
