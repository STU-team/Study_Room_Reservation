package com.studyroom.ui;

import com.studyroom.service.BusinessException;
import com.studyroom.service.ReservationService;
import com.studyroom.service.ServiceFactory;

import javax.swing.*;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * 程序入口：初始化数据、全局爽约定时器（守护线程，全局唯一定时扫描）、启动登录窗口。
 */
public final class AppMain {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        UiTheme.installGlobalFonts();

        ServiceFactory.configure(Path.of("data"));
        ReservationService reservations = ServiceFactory.getReservationService();

        try {
            int count = reservations.handleNoShow(LocalDateTime.now());
            if (count > 0) {
                System.out.println("[系统] 启动时处理 " + count + " 条爽约记录");
            }
        } catch (BusinessException e) {
            System.out.println("[系统] 爽约扫描异常：" + e.getMessage());
        }

        // 全局爽约定时器：守护线程，每 60 分钟扫描一次，与任何窗口无关
        java.util.Timer globalTimer = new java.util.Timer("no-show-scanner", true);
        globalTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                try {
                    int count = reservations.handleNoShow(LocalDateTime.now());
                    if (count > 0) {
                        System.out.println("[系统] 定时扫描处理 " + count + " 条爽约记录");
                    }
                } catch (Exception ignored) {}
            }
        }, 60 * 60 * 1000L, 60 * 60 * 1000L);

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}