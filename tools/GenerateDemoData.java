import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 生成 A-8 演示数据。
 * 预约日期全部按运行当天推算（未来 ≤ 7 天、历史记录在过去 1~2 天），
 * 保证任何一天运行都能直接演示签到、签退、爽约扫描。
 *
 * 用法：java tools/GenerateDemoData.java [输出目录，默认 data]
 */
public class GenerateDemoData {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) throws IOException {
        Path dir = Path.of(args.length > 0 ? args[0] : "data");
        Files.createDirectories(dir);
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate before = today.minusDays(2);
        LocalDate soon = today.plusDays(3);

        write(dir, "seats.csv", "seatNo,typeName,status",
                "A01,靠窗区,AVAILABLE",
                "A02,靠窗区,RESERVED",
                "B01,沙发区,OCCUPIED",
                "B02,沙发区,CLOSED",
                "C01,研讨区,RESERVED");
        write(dir, "seat_types.csv", "typeName,seatCount", "靠窗区,2", "沙发区,2", "研讨区,1");
        write(dir, "students.csv", "name,studentId,phone,creditScore",
                "张三,20240001,13800000000,99",
                "李四,20240002,13900000000,95",
                "王五,20240003,13700000000,70");
        write(dir, "users.csv", "username,password,role",
                "admin,123456,SYS_ADMIN",
                "duty,123456,DUTY_ADMIN",
                "20240001,123456,STUDENT");

        String r1 = resNo(soon, 1);
        String r2 = resNo(yesterday, 1);
        String r3 = resNo(yesterday, 2);
        String r4 = resNo(before, 1);
        String r5 = resNo(before, 2);
        String r6 = resNo(yesterday, 3);
        write(dir, "reservations.csv", "resNo,studentName,studentId,phone,seatNo,resDate,startTime,endTime,status,createTime",
                r1 + ",张三,20240001,13800000000,A02," + day(soon, "09:00", "11:00") + ",BOOKED," + at(yesterday, "20:10"),
                r2 + ",李四,20240002,13900000000,B01," + day(yesterday, "09:00", "11:00") + ",CHECKED_IN," + at(before, "20:20"),
                r3 + ",张三,20240001,13800000000,A01," + day(yesterday, "14:00", "16:00") + ",CHECKED_OUT," + at(yesterday, "09:00"),
                r4 + ",李四,20240002,13900000000,B02," + day(before, "10:00", "12:00") + ",CANCELLED," + at(before, "21:00"),
                r5 + ",王五,20240003,13700000000,A01," + day(before, "15:00", "17:00") + ",NO_SHOW," + at(before, "10:00"),
                r6 + ",王五,20240003,13700000000,C01," + day(yesterday, "08:00", "10:00") + ",BOOKED," + at(before, "20:00"));

        String y = yesterday.format(DATE);
        write(dir, "checkin_records.csv", "recordNo,resNo,actualStart,actualEnd,penalty,createTime",
                recordNo(yesterday, 1) + "," + r2 + "," + y + " 09:02,,," + y + " 09:05",
                recordNo(yesterday, 2) + "," + r3 + "," + y + " 14:03," + y + " 16:10,1," + y + " 14:05");

        write(dir, "credit_score_logs.csv", "logId,studentId,reservationId,changeValue,reason,createTime",
                "1,20240001," + id(r3) + ",-1,LATE_CHECKOUT," + y + " 16:10",
                "2,20240003," + id(r5) + ",-5,NO_SHOW," + before.format(DATE) + " 17:15");

        System.out.println("已生成 7 个 CSV 到 " + dir.toAbsolutePath());
        System.out.println("签到用 " + r1 + "（" + soon + " 09:00），签退用 " + r2 + "，爽约扫描用 " + r6);
    }

    private static String day(LocalDate date, String start, String end) {
        return date.format(DATE) + "," + start + "," + end;
    }

    private static String at(LocalDate date, String time) {
        return date.format(DATE) + " " + time;
    }

    private static String resNo(LocalDate date, int seq) {
        return "RES" + date.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + String.format("%03d", seq);
    }

    private static String recordNo(LocalDate date, int seq) {
        return "REC" + date.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + String.format("%03d", seq);
    }

    /** 与 CsvMapper.id 相同的 FNV-1a 63 位散列，用于信用分流水反查预约。 */
    private static long id(String key) {
        long h = 0xcbf29ce484222325L;
        for (byte b : key.getBytes(StandardCharsets.UTF_8)) {
            h = (h ^ (b & 0xff)) * 0x100000001b3L;
        }
        return h & 0x7FFFFFFFFFFFFFFFL;
    }

    private static void write(Path dir, String name, String... lines) throws IOException {
        Files.writeString(dir.resolve(name), String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    }
}
