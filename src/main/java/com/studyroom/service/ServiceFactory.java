package com.studyroom.service;

import com.studyroom.dao.CheckinRecordDao;
import com.studyroom.dao.CreditScoreLogDao;
import com.studyroom.dao.ReservationDao;
import com.studyroom.dao.SeatDao;
import com.studyroom.dao.StudentDao;
import com.studyroom.dao.UserDao;
import com.studyroom.dao.impl.CsvCheckinRecordDao;
import com.studyroom.dao.impl.CsvCreditScoreLogDao;
import com.studyroom.dao.impl.CsvReservationDao;
import com.studyroom.dao.impl.CsvSeatDao;
import com.studyroom.dao.impl.CsvStudentDao;
import com.studyroom.dao.impl.CsvUserDao;
import com.studyroom.service.impl.AuthServiceImpl;
import com.studyroom.service.impl.ReservationServiceImpl;
import com.studyroom.service.impl.SeatServiceImpl;
import com.studyroom.util.DataInitializer;

import java.io.IOException;
import java.nio.file.Path;

/** 前端唯一入口：按依赖顺序装配 DAO 与 Service，全部单例复用同一份文件句柄。 */
public final class ServiceFactory {
    private static Path dataDirectory = Path.of("data");
    private static SeatDao seatDao;
    private static StudentDao studentDao;
    private static UserDao userDao;
    private static ReservationDao reservationDao;
    private static CheckinRecordDao checkinRecordDao;
    private static CreditScoreLogDao creditScoreLogDao;
    private static ReservationService reservationService;
    private static SeatService seatService;
    private static AuthService authService;

    private ServiceFactory() {
    }

    /** 指定数据目录（默认 data/），已装配的实例随之作废，下次获取时按新目录重建。 */
    public static synchronized void configure(Path directory) {
        if (directory == null) throw new BusinessException("PARAM", "数据目录不能为空");
        dataDirectory = directory;
        seatDao = null;
        studentDao = null;
        userDao = null;
        reservationDao = null;
        checkinRecordDao = null;
        creditScoreLogDao = null;
        reservationService = null;
        seatService = null;
        authService = null;
    }

    public static synchronized ReservationService getReservationService() {
        if (reservationService == null) {
            assemble();
            reservationService = new ReservationServiceImpl(seatDao, reservationDao, studentDao,
                    checkinRecordDao, creditScoreLogDao);
        }
        return reservationService;
    }

    public static synchronized SeatService getSeatService() {
        if (seatService == null) {
            assemble();
            seatService = new SeatServiceImpl(seatDao);
        }
        return seatService;
    }

    public static synchronized AuthService getAuthService() {
        if (authService == null) {
            assemble();
            authService = new AuthServiceImpl(userDao);
        }
        return authService;
    }

    /** 首次装配时建表并写入示例数据（契约第八节）；依赖 CsvReservationDao 需要学生档案，故按序创建。 */
    private static void assemble() {
        if (seatDao != null) return;
        try {
            DataInitializer.initialize(dataDirectory);
            DataInitializer.seedSamples(dataDirectory);
        } catch (IOException e) {
            throw new BusinessException("DATA_INIT", "数据目录初始化失败：" + e.getMessage());
        }
        seatDao = new CsvSeatDao(dataDirectory);
        studentDao = new CsvStudentDao(dataDirectory);
        userDao = new CsvUserDao(dataDirectory);
        checkinRecordDao = new CsvCheckinRecordDao(dataDirectory);
        creditScoreLogDao = new CsvCreditScoreLogDao(dataDirectory);
        reservationDao = new CsvReservationDao(dataDirectory, studentDao);
    }
}
