package com.studyroom.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CsvUtilTest {
    @TempDir
    Path tempDir;

    @Test
    void csvRoundTripSupportsChineseCommaQuoteAndNewline() throws IOException {
        Path file = tempDir.resolve("roundtrip.csv");
        List<List<String>> rows = List.of(
                List.of("name", "note", "empty"),
                List.of("张三", "靠窗,安静\"优先\"\n第二行", "")
        );

        CsvUtil.writeAll(file, rows);

        assertEquals(rows, CsvUtil.readAll(file));
    }

    @Test
    void malformedQuotedFieldIsRejected() {
        assertThrows(IOException.class, () -> CsvUtil.parse("\"closed\"unexpected"));
        assertThrows(IOException.class, () -> CsvUtil.parse("\"not-closed"));
    }

    @Test
    void initializerCreatesSevenFilesAndDoesNotOverwriteExistingData() throws IOException {
        Path data = tempDir.resolve("data");
        DataInitializer.initialize(data);
        Path students = data.resolve("students.csv");
        CsvUtil.appendRow(students, List.of("张三", "20240001", "13800000000", "100"));

        DataInitializer.initialize(data);

        List<List<String>> contents = CsvUtil.readAll(students);
        assertEquals(List.of("name", "studentId", "phone", "creditScore"), contents.get(0));
        assertEquals(List.of("张三", "20240001", "13800000000", "100"), contents.get(1));
        try (Stream<Path> files = Files.list(data)) {
            assertEquals(7, files.filter(Files::isRegularFile).count());
        }
    }

    @Test
    void initializerRejectsUnexpectedExistingHeaderInsteadOfOverwriting() throws IOException {
        Path data = tempDir.resolve("data");
        Files.createDirectories(data);
        Files.writeString(data.resolve("seats.csv"), "id,type,status\n1,A,AVAILABLE\n");

        assertThrows(IOException.class, () -> DataInitializer.initialize(data));
        assertTrue(Files.readString(data.resolve("seats.csv")).startsWith("id,type,status"));
    }
}
