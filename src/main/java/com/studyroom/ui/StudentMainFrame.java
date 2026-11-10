package com.studyroom.ui;

import com.studyroom.entity.Reservation;
import com.studyroom.entity.ReservationStatus;
import com.studyroom.entity.Seat;
import com.studyroom.entity.SeatStatus;
import com.studyroom.entity.Student;
import com.studyroom.entity.User;
import com.studyroom.service.AuthService;
import com.studyroom.service.BusinessException;
import com.studyroom.service.ReservationService;
import com.studyroom.service.SeatService;
import com.studyroom.service.ServiceFactory;
import com.studyroom.service.StudentService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 学生主窗口：顶部导航、中间卡片区、底部 3 个标签页。 */
public class StudentMainFrame extends JFrame {

    private final User currentUser;
    private final SeatService seatService;
    private final ReservationService reservationService;
    private final StudentService studentService;
    private final AuthService authService;

    private JLabel creditLabel;
    private CardLayout cardLayout;
    private JPanel centerPanel;
    private JPanel bottomTabBar;

    private DefaultTableModel seatTableModel;
    private DefaultTableModel availTableModel;
    private DefaultTableModel myResvTableModel;
    private JTable availTable;
    private JTable myResvTable;
    private JComboBox<String> dateCombo;
    private JComboBox<String> startCombo;
    private JComboBox<String> endCombo;
    private JComboBox<String> roomCombo;

    private static final String[] CARDS = {"seat", "reserve", "my"};
    private static final SimpleDateFormat SDF_DATETIME = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int COL_SEAT_STATUS = 2;
    private static final int COL_RES_STATUS = 8;

    public StudentMainFrame(User user) {
        this.currentUser = user;
        this.seatService = ServiceFactory.getSeatService();
        this.reservationService = ServiceFactory.getReservationService();
        this.studentService = ServiceFactory.getStudentService();
        this.authService = ServiceFactory.getAuthService();

        setTitle("自习室预约系统 - 学生端");
        setSize(1050, 720);
        setMinimumSize(new Dimension(900, 620));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(UiTheme.PANEL);
        setLayout(new BorderLayout());

        add(createTopNav(), BorderLayout.NORTH);

        cardLayout = new CardLayout();
        centerPanel = new JPanel(cardLayout);
        centerPanel.setBackground(UiTheme.PANEL);
        centerPanel.add(createSeatPanel(), "seat");
        centerPanel.add(createReservePanel(), "reserve");
        centerPanel.add(createMyReservationPanel(), "my");
        add(centerPanel, BorderLayout.CENTER);

        bottomTabBar = UiTheme.tabBar(
                UiTheme.tabButton("座位总览", e -> switchTab("seat")),
                UiTheme.tabButton("预约座位", e -> switchTab("reserve")),
                UiTheme.tabButton("我的预约", e -> switchTab("my")));
        add(bottomTabBar, BorderLayout.SOUTH);

        switchTab("seat");
    }

    // ==================== 顶部导航 ====================

    private JPanel createTopNav() {
        JPanel nav = new JPanel(new BorderLayout());
        nav.setBackground(UiTheme.HEADER);
        nav.setBorder(new EmptyBorder(10, 16, 10, 16));

        JLabel title = new JLabel("自习室预约系统");
        title.setFont(UiTheme.font(Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        nav.add(title, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        creditLabel = new JLabel();
        creditLabel.setFont(UiTheme.font(Font.BOLD, 14));
        creditLabel.setForeground(Color.WHITE);
        refreshCreditDisplay();
        right.add(creditLabel);

        JLabel separator = new JLabel("|");
        separator.setForeground(new Color(255, 255, 255, 60));
        right.add(separator);

        JLabel userLabel = new JLabel(currentUser.getUsername() + "（学生）");
        userLabel.setFont(UiTheme.font(Font.PLAIN, 13));
        userLabel.setForeground(new Color(255, 255, 255, 200));
        right.add(userLabel);
        right.add(UiTheme.navButton("退出", UiTheme.RED, e -> logout()));

        nav.add(right, BorderLayout.EAST);
        return nav;
    }

    private void logout() {
        authService.logout();
        dispose();
        new LoginFrame().setVisible(true);
    }

    private void refreshCreditDisplay() {
        try {
            Student student = studentService.findByStudentId(currentUser.getUsername());
            int score = student.getCreditScore();
            creditLabel.setText("信用分：" + score);
            creditLabel.setForeground(score < 80 ? new Color(255, 235, 59) : Color.WHITE);
        } catch (BusinessException e) {
            creditLabel.setText("信用分：--");
        }
    }

    // ==================== 座位总览 ====================

    private JPanel createSeatPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UiTheme.PANEL);
        panel.setBorder(new EmptyBorder(4, 12, 8, 12));
        panel.add(createSeatLegend(), BorderLayout.NORTH);

        seatTableModel = new DefaultTableModel(new String[]{"座位号", "区域", "状态"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable seatTable = TableFactory.create(seatTableModel, 48);
        seatTable.getColumnModel().getColumn(COL_SEAT_STATUS).setCellRenderer(TableFactory.seatStatusRenderer());
        panel.add(TableFactory.scroll(seatTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSeatLegend() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 7));
        legend.setBackground(Color.WHITE);
        legend.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDER),
                new EmptyBorder(0, 8, 0, 8)));
        legend.add(legendItem("可用", SeatStatus.AVAILABLE));
        legend.add(legendItem("已预约", SeatStatus.RESERVED));
        legend.add(legendItem("使用中", SeatStatus.OCCUPIED));
        legend.add(legendItem("维修", SeatStatus.CLOSED));
        return legend;
    }

    private JLabel legendItem(String text, SeatStatus status) {
        JLabel label = new JLabel("● " + text);
        label.setFont(UiTheme.font(Font.PLAIN, 13));
        label.setForeground(TableFactory.seatColor(status));
        return label;
    }

    private void refreshSeatTable() {
        seatTableModel.setRowCount(0);
        for (Seat seat : seatService.listAllSeats()) {
            seatTableModel.addRow(new Object[]{seat.getSeatNo(), seat.getRoomName(), seat.getStatus()});
        }
    }

    // ==================== 预约座位 ====================

    private JPanel createReservePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UiTheme.PANEL);
        panel.setBorder(new EmptyBorder(4, 12, 8, 12));

        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        search.setBackground(Color.WHITE);
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER), "搜索条件");
        titledBorder.setTitleFont(UiTheme.font(Font.BOLD, 13));
        titledBorder.setTitleColor(UiTheme.TEXT);
        search.setBorder(BorderFactory.createCompoundBorder(titledBorder, new EmptyBorder(2, 8, 2, 8)));

        Font labelFont = UiTheme.font(Font.PLAIN, 14);
        search.add(label("日期：", labelFont));
        dateCombo = new JComboBox<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            dateCombo.addItem(today.plusDays(i).format(DATE_FMT));
        }
        configureCombo(dateCombo, 112);
        dateCombo.addActionListener(e -> updateTimeCombos());
        search.add(dateCombo);

        search.add(label("开始：", labelFont));
        startCombo = new JComboBox<>();
        configureCombo(startCombo, 82);
        startCombo.addActionListener(e -> updateEndCombo());
        search.add(startCombo);

        search.add(label("结束：", labelFont));
        endCombo = new JComboBox<>();
        configureCombo(endCombo, 82);
        search.add(endCombo);

        search.add(label("区域：", labelFont));
        roomCombo = new JComboBox<>();
        roomCombo.addItem("不限");
        Set<String> rooms = new LinkedHashSet<>();
        for (Seat seat : seatService.listAllSeats()) {
            rooms.add(seat.getRoomName());
        }
        rooms.forEach(roomCombo::addItem);
        configureCombo(roomCombo, 100);
        search.add(roomCombo);

        search.add(UiTheme.primaryButton("搜索", UiTheme.BLUE, e -> doSearch()));
        panel.add(search, BorderLayout.NORTH);

        availTableModel = new DefaultTableModel(new String[]{"座位号", "区域"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        availTable = TableFactory.create(availTableModel, 40, "设置条件后点击“搜索”查看可用座位");
        panel.add(TableFactory.scroll(availTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        actions.setOpaque(false);
        actions.add(UiTheme.primaryButton("预约选中座位", UiTheme.GREEN, e -> doReserve()));
        JLabel hint = new JLabel("选条件 → 搜索 → 选中一行 → 点预约");
        hint.setFont(UiTheme.font(Font.PLAIN, 11));
        hint.setForeground(UiTheme.TEXT_MUTED);
        actions.add(hint);
        panel.add(actions, BorderLayout.SOUTH);

        updateTimeCombos();
        return panel;
    }

    private JLabel label(String text, Font font) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        return label;
    }

    private void configureCombo(JComboBox<String> combo, int width) {
        combo.setFont(UiTheme.font(Font.PLAIN, 14));
        combo.setBackground(Color.WHITE);
        combo.setPreferredSize(new Dimension(width, 30));
    }

    /** 只保留当前时间之后的半小时时段；当天已无可用时段时两个下拉框都为空。 */
    private void updateTimeCombos() {
        String selectedDate = (String) dateCombo.getSelectedItem();
        boolean isToday = selectedDate != null && selectedDate.equals(LocalDate.now().format(DATE_FMT));
        LocalTime now = LocalTime.now();
        String previous = (String) startCombo.getSelectedItem();

        startCombo.removeAllItems();
        for (int hour = 0; hour < 24; hour++) {
            for (int minute = 0; minute < 60; minute += 30) {
                LocalTime time = LocalTime.of(hour, minute);
                if (isToday && !time.isAfter(now)) {
                    continue;
                }
                startCombo.addItem(String.format("%02d:%02d", hour, minute));
            }
        }
        if (startCombo.getItemCount() > 0) {
            int restore = -1;
            for (int i = 0; i < startCombo.getItemCount(); i++) {
                if (startCombo.getItemAt(i).equals(previous)) {
                    restore = i;
                    break;
                }
            }
            startCombo.setSelectedIndex(restore >= 0 ? restore : 0);
        }
        updateEndCombo();
    }

    private void updateEndCombo() {
        String previous = (String) endCombo.getSelectedItem();
        endCombo.removeAllItems();
        String start = (String) startCombo.getSelectedItem();
        if (start == null) {
            return;
        }
        int startMinutes = Integer.parseInt(start.substring(0, 2)) * 60 + Integer.parseInt(start.substring(3));
        for (int hour = 0; hour < 24; hour++) {
            for (int minute = 0; minute < 60; minute += 30) {
                if (hour * 60 + minute <= startMinutes) {
                    continue;
                }
                String value = String.format("%02d:%02d", hour, minute);
                endCombo.addItem(value);
                if (value.equals(previous)) {
                    endCombo.setSelectedItem(value);
                }
            }
        }
        if (endCombo.getItemCount() > 0 && endCombo.getSelectedItem() == null) {
            endCombo.setSelectedIndex(0);
        }
    }

    private void doSearch() {
        if (startCombo.getSelectedItem() == null || endCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "今日已没有可预约的时段，请选择之后的日期。",
                    "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            LocalDate date = LocalDate.parse((String) dateCombo.getSelectedItem(), DATE_FMT);
            LocalTime start = LocalTime.parse((String) startCombo.getSelectedItem());
            LocalTime end = LocalTime.parse((String) endCombo.getSelectedItem());
            String room = (String) roomCombo.getSelectedItem();
            List<Seat> seats = reservationService.listAvailableSeats(date, start, end,
                    "不限".equals(room) ? null : room);

            availTableModel.setRowCount(0);
            for (Seat seat : seats) {
                availTableModel.addRow(new Object[]{seat.getSeatNo(), seat.getRoomName()});
            }
            TableFactory.setEmptyText(availTable,
                    seats.isEmpty() ? "该时段没有可用座位" : "暂无数据");
            if (seats.isEmpty()) {
                JOptionPane.showMessageDialog(this, "该时段没有可用座位", "提示",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (BusinessException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "查询失败", JOptionPane.WARNING_MESSAGE);
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "日期或时间格式不正确，请重新选择。",
                    "查询失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doReserve() {
        int row = availTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "请先选中一个可用座位", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String seatNo = String.valueOf(availTable.getValueAt(row, 0));
        String dateText = (String) dateCombo.getSelectedItem();
        String startText = (String) startCombo.getSelectedItem();
        String endText = (String) endCombo.getSelectedItem();
        if (dateText == null || startText == null || endText == null) {
            JOptionPane.showMessageDialog(this, "请先选择完整的日期和时段", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "确认预约？\n座位：" + seatNo + "\n日期：" + dateText
                        + "\n时段：" + startText + " - " + endText,
                "确认预约", JOptionPane.OK_CANCEL_OPTION);
        if (confirm != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            Student student = studentService.findByStudentId(currentUser.getUsername());
            Reservation reservation = reservationService.reserve(seatNo, student,
                    LocalDate.parse(dateText, DATE_FMT), LocalTime.parse(startText), LocalTime.parse(endText));
            JOptionPane.showMessageDialog(this,
                    "预约成功！\n预约号：" + reservation.getResNo()
                            + "\n签到截止：" + SDF_DATETIME.format(reservation.getSignInDeadline()),
                    "预约成功", JOptionPane.INFORMATION_MESSAGE);

            // 成功后立即从结果表中移除，避免重复点击造成重复预约
            for (int i = availTableModel.getRowCount() - 1; i >= 0; i--) {
                if (seatNo.equals(String.valueOf(availTableModel.getValueAt(i, 0)))) {
                    availTableModel.removeRow(i);
                }
            }
            TableFactory.setEmptyText(availTable,
                    availTableModel.getRowCount() == 0 ? "该时段没有可用座位" : "暂无数据");
            refreshAll();
        } catch (BusinessException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "预约失败", JOptionPane.WARNING_MESSAGE);
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "日期或时间格式不正确，请重新选择。",
                    "预约失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==================== 我的预约 ====================

    private JPanel createMyReservationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UiTheme.PANEL);
        panel.setBorder(new EmptyBorder(4, 12, 8, 12));

        String[] columns = {"预约号", "座位", "座位状态", "姓名", "手机", "开始时间", "结束时间", "签到截止", "状态", "提交时间"};
        myResvTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        myResvTable = TableFactory.create(myResvTableModel, 54, "还没有预约记录");
        myResvTable.getColumnModel().getColumn(COL_SEAT_STATUS).setCellRenderer(TableFactory.seatStatusRenderer());
        myResvTable.getColumnModel().getColumn(COL_RES_STATUS).setCellRenderer(TableFactory.reservationStatusRenderer());
        panel.add(TableFactory.scroll(myResvTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        actions.setOpaque(false);
        actions.add(UiTheme.primaryButton("取消预约", UiTheme.RED, e -> doCancel()));
        actions.add(UiTheme.secondaryButton("刷新", UiTheme.BLUE, e -> refreshMyReservations()));
        JLabel hint = new JLabel("仅“待签到”状态的预约可以取消");
        hint.setFont(UiTheme.font(Font.PLAIN, 11));
        hint.setForeground(UiTheme.TEXT_MUTED);
        actions.add(hint);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshMyReservations() {
        List<Reservation> list = reservationService.listReservations(currentUser.getUsername());
        Map<String, SeatStatus> statusBySeat = new HashMap<>();
        for (Seat seat : seatService.listAllSeats()) {
            statusBySeat.put(seat.getSeatNo(), seat.getStatus());
        }
        myResvTableModel.setRowCount(0);
        for (Reservation reservation : list) {
            myResvTableModel.addRow(new Object[]{
                    reservation.getResNo(), reservation.getSeatNo(),
                    statusBySeat.get(reservation.getSeatNo()), reservation.getStudentName(),
                    reservation.getPhone(),
                    SDF_DATETIME.format(reservation.getStartTime()),
                    SDF_DATETIME.format(reservation.getEndTime()),
                    SDF_DATETIME.format(reservation.getSignInDeadline()),
                    reservation.getStatus(),
                    SDF_DATETIME.format(reservation.getCreateTime())
            });
        }
    }

    private void doCancel() {
        int row = myResvTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "请先选中一条预约记录", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String resNo = String.valueOf(myResvTable.getValueAt(row, 0));
        if (myResvTable.getValueAt(row, COL_RES_STATUS) != ReservationStatus.BOOKED) {
            JOptionPane.showMessageDialog(this, "只能取消“待签到”状态的预约", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "确认取消 " + resNo + "？",
                "确认", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            reservationService.cancelReservation(resNo);
            JOptionPane.showMessageDialog(this, "已取消 " + resNo);
            refreshAll();
        } catch (BusinessException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "取消失败", JOptionPane.WARNING_MESSAGE);
            refreshMyReservations();
        }
    }

    // ==================== 通用 ====================

    private void switchTab(String card) {
        cardLayout.show(centerPanel, card);
        int index = 0;
        for (int i = 0; i < CARDS.length; i++) {
            if (CARDS[i].equals(card)) {
                index = i;
                break;
            }
        }
        UiTheme.activateTab(bottomTabBar, index);

        if ("seat".equals(card)) {
            refreshSeatTable();
        } else if ("my".equals(card)) {
            refreshMyReservations();
        }
    }

    private void refreshAll() {
        refreshSeatTable();
        refreshMyReservations();
        refreshCreditDisplay();
    }
}
