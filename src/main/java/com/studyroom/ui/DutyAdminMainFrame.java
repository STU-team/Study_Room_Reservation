package com.studyroom.ui;

import com.studyroom.entity.CheckinRecord;
import com.studyroom.entity.Reservation;
import com.studyroom.entity.ReservationStatus;
import com.studyroom.entity.User;
import com.studyroom.service.AuthService;
import com.studyroom.service.BusinessException;
import com.studyroom.service.ReservationService;
import com.studyroom.service.ServiceFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 值班管理员：顶部导航、左侧操作栏、中间表格、底部 Tab。 */
public class DutyAdminMainFrame extends JFrame {

    private final User currentUser;
    private final ReservationService reservationService;
    private final AuthService authService;

    private DefaultTableModel allResvModel;
    private DefaultTableModel seatedModel;
    private JTable allResvTable;
    private JTable seatedTable;
    private JTextField searchField;
    private CardLayout cardLayout;
    private JPanel centerPanel;
    private JPanel bottomTabBar;

    private String currentCard = "all";

    private static final String[] CARDS = {"all", "seated"};
    private static final SimpleDateFormat SDF_DATETIME = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private static final SimpleDateFormat SDF_DATE = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat SDF_TIME = new SimpleDateFormat("HH:mm");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public DutyAdminMainFrame(User user) {
        this.currentUser = user;
        this.reservationService = ServiceFactory.getReservationService();
        this.authService = ServiceFactory.getAuthService();

        setTitle("自习室预约系统 - 值班管理员");
        setSize(1150, 750);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(UiTheme.PANEL);
        setLayout(new BorderLayout());

        add(createTopNav(), BorderLayout.NORTH);

        JPanel middle = new JPanel(new BorderLayout());
        middle.setBackground(UiTheme.PANEL);
        middle.add(createSidebar(), BorderLayout.WEST);

        cardLayout = new CardLayout();
        centerPanel = new JPanel(cardLayout);
        centerPanel.setBackground(UiTheme.PANEL);
        centerPanel.add(createAllResvPanel(), "all");
        centerPanel.add(createSeatedPanel(), "seated");
        middle.add(centerPanel, BorderLayout.CENTER);
        add(middle, BorderLayout.CENTER);

        bottomTabBar = UiTheme.tabBar(
                UiTheme.tabButton("全部预约", e -> switchTab("all")),
                UiTheme.tabButton("在座名单", e -> switchTab("seated")));
        add(bottomTabBar, BorderLayout.SOUTH);

        switchTab("all");
    }

    // ==================== 顶部导航 ====================

    private JPanel createTopNav() {
        JPanel nav = new JPanel(new BorderLayout());
        nav.setBackground(UiTheme.HEADER);
        nav.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        JLabel title = new JLabel("自习室预约系统");
        title.setFont(UiTheme.font(Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        nav.add(title, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        JLabel userLabel = new JLabel(currentUser.getUsername() + "（值班管理员）");
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

    // ==================== 左侧操作栏 ====================

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UiTheme.SIDEBAR);
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, UiTheme.BORDER_DARK),
                BorderFactory.createEmptyBorder(16, 14, 16, 14)));
        sidebar.setPreferredSize(new Dimension(150, 0));

        JLabel label = new JLabel("操作");
        label.setFont(UiTheme.font(Font.BOLD, 14));
        label.setForeground(UiTheme.HEADER);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(label);
        sidebar.add(Box.createVerticalStrut(12));

        sidebar.add(UiTheme.sideButton("签到登记", e -> doSignInSelected()));
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(UiTheme.sideButton("签退释放", e -> doSignOutSelected()));
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(UiTheme.sideButton("预约搜索", e -> focusSearch()));
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(UiTheme.sideButton("刷新列表", e -> refreshAll()));

        sidebar.add(Box.createVerticalStrut(16));
        JLabel hint = new JLabel("<html><body style='width:110px'>双击表格记录<br>可快捷签到/签退</body></html>");
        hint.setFont(UiTheme.font(Font.PLAIN, 11));
        hint.setForeground(UiTheme.TEXT_MUTED);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(hint);
        return sidebar;
    }

    /** 侧栏操作可能发生在“在座名单”页，先切回列表再操作，避免作用在不可见的选中行上。 */
    private void ensureAllTab() {
        if (!"all".equals(currentCard)) {
            switchTab("all");
        }
    }

    private void focusSearch() {
        ensureAllTab();
        searchField.requestFocusInWindow();
        searchField.selectAll();
    }

    // ==================== 全部预约 ====================

    private JPanel createAllResvPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UiTheme.PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));

        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        search.setBackground(UiTheme.WHITE);
        search.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        JLabel label = new JLabel("预约号搜索：");
        label.setFont(UiTheme.font(Font.PLAIN, 14));
        search.add(label);
        searchField = new JTextField(18);
        searchField.setFont(UiTheme.font(Font.PLAIN, 15));
        searchField.addActionListener(e -> doSearchResv());
        search.add(searchField);
        search.add(UiTheme.smallButton("查找", UiTheme.BLUE, e -> doSearchResv()));
        search.add(UiTheme.smallButton("全部", UiTheme.GREY, e -> refreshAllResv()));
        JLabel hint = new JLabel("双击记录可快捷签到/签退");
        hint.setFont(UiTheme.font(Font.PLAIN, 11));
        hint.setForeground(UiTheme.TEXT_MUTED);
        search.add(hint);
        panel.add(search, BorderLayout.NORTH);

        String[] columns = {"预约号", "座位", "学号", "姓名", "手机", "日期", "开始时间", "结束时间", "签到截止", "状态"};
        allResvModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        allResvTable = TableFactory.create(allResvModel, 54, "暂无预约记录");
        allResvTable.getColumnModel().getColumn(9).setCellRenderer(TableFactory.reservationStatusRenderer());
        allResvTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleRowDoubleClick(allResvTable);
                }
            }
        });
        panel.add(TableFactory.scroll(allResvTable), BorderLayout.CENTER);
        return panel;
    }

    private void doSearchResv() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            refreshAllResv();
            return;
        }
        try {
            Reservation reservation = reservationService.findReservation(keyword);
            allResvModel.setRowCount(0);
            addResvRow(reservation);
            TableFactory.setEmptyText(allResvTable, "没有找到该预约号");
        } catch (BusinessException e) {
            allResvModel.setRowCount(0);
            TableFactory.setEmptyText(allResvTable, "没有找到该预约号");
            JOptionPane.showMessageDialog(this, e.getMessage(), "未找到", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void refreshAllResv() {
        allResvModel.setRowCount(0);
        for (Reservation reservation : reservationService.listAllReservations()) {
            addResvRow(reservation);
        }
        TableFactory.setEmptyText(allResvTable, "暂无预约记录");
    }

    private void addResvRow(Reservation reservation) {
        allResvModel.addRow(new Object[]{
                reservation.getResNo(), reservation.getSeatNo(), reservation.getStudentId(),
                reservation.getStudentName(), reservation.getPhone(),
                SDF_DATE.format(reservation.getStartTime()),
                SDF_TIME.format(reservation.getStartTime()),
                SDF_TIME.format(reservation.getEndTime()),
                SDF_DATETIME.format(reservation.getSignInDeadline()),
                reservation.getStatus()
        });
    }

    // ==================== 签到 / 签退 ====================

    private void handleRowDoubleClick(JTable table) {
        int row = table.getSelectedRow();
        if (row == -1) {
            return;
        }
        String resNo = String.valueOf(table.getValueAt(row, 0));
        Object status = table.getValueAt(row, 9);
        if (status == ReservationStatus.BOOKED) {
            doSignIn(resNo);
        } else if (status == ReservationStatus.CHECKED_IN) {
            doSignOut(resNo);
        }
    }

    private void doSignInSelected() {
        ensureAllTab();
        int row = allResvTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "请先在“全部预约”中选中一条记录",
                    "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        doSignIn(String.valueOf(allResvTable.getValueAt(row, 0)));
    }

    private void doSignIn(String resNo) {
        Reservation reservation;
        try {
            reservation = reservationService.findReservation(resNo);
        } catch (BusinessException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "签到失败", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (reservation.getStatus() != ReservationStatus.BOOKED) {
            JOptionPane.showMessageDialog(this, "只能对“待签到”状态的预约执行签到",
                    "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        LocalDateTime start = toLocalDateTime(reservation.getStartTime());
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(start.minusMinutes(5))) {
            JOptionPane.showMessageDialog(this,
                    "预约开始时间为 " + start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                            + "，最早可提前 5 分钟签到。\n当前时间：" + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    "尚未到签到时间", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "确认签到？\n预约号：" + resNo
                        + "\n座位：" + reservation.getSeatNo()
                        + "\n开始时间：" + SDF_DATETIME.format(reservation.getStartTime()),
                "确认签到", JOptionPane.OK_CANCEL_OPTION);
        if (confirm != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            reservationService.signIn(resNo);
            JOptionPane.showMessageDialog(this, "签到成功：" + resNo);
            refreshAll();
        } catch (BusinessException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "签到失败", JOptionPane.WARNING_MESSAGE);
            refreshAll();
        }
    }

    private void doSignOutSelected() {
        ensureAllTab();
        int row = allResvTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "请先在“全部预约”中选中一条记录",
                    "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        doSignOut(String.valueOf(allResvTable.getValueAt(row, 0)));
    }

    private void doSignOut(String resNo) {
        Reservation reservation;
        try {
            reservation = reservationService.findReservation(resNo);
        } catch (BusinessException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "签退失败", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            JOptionPane.showMessageDialog(this, "只能对“使用中”状态的预约执行签退",
                    "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JSpinner spinner = new JSpinner(new SpinnerDateModel());
        spinner.setEditor(new JSpinner.DateEditor(spinner, "HH:mm"));
        spinner.setValue(new Date());
        JPanel input = new JPanel(new BorderLayout(8, 8));
        input.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        input.add(new JLabel("实际签退时间："), BorderLayout.NORTH);
        input.add(spinner, BorderLayout.CENTER);

        int confirm = JOptionPane.showConfirmDialog(this, input,
                "签退 " + resNo, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (confirm != JOptionPane.OK_OPTION) {
            return;
        }
        LocalTime actualEnd = LocalTime.parse(SDF_TIME.format((Date) spinner.getValue()), TIME_FMT);
        try {
            CheckinRecord record = reservationService.signOut(resNo, actualEnd);
            String message = "签退成功：" + resNo + "\n座位：" + reservation.getSeatNo();
            if (record.getPenalty() != null && record.getPenalty() > 0) {
                message += "\n超时扣分：" + record.getPenalty() + " 分";
            }
            JOptionPane.showMessageDialog(this, message);
            refreshAll();
        } catch (BusinessException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "签退失败", JOptionPane.WARNING_MESSAGE);
            refreshAll();
        }
    }

    // ==================== 在座名单 ====================

    private JPanel createSeatedPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UiTheme.PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));

        String[] columns = {"记录号", "预约号", "座位", "姓名", "手机", "签到时间", "超时扣分"};
        seatedModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        seatedTable = TableFactory.create(seatedModel, 54, "当前没有在座学生");
        seatedTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && seatedTable.getSelectedRow() != -1) {
                    doSignOut(String.valueOf(seatedTable.getValueAt(seatedTable.getSelectedRow(), 1)));
                }
            }
        });
        panel.add(TableFactory.scroll(seatedTable), BorderLayout.CENTER);
        return panel;
    }

    private void refreshSeated() {
        // 一次取出全部预约建索引，避免逐条 findReservation 造成的重复查表
        Map<String, Reservation> reservationByNo = new HashMap<>();
        for (Reservation reservation : reservationService.listAllReservations()) {
            reservationByNo.put(reservation.getResNo(), reservation);
        }
        List<CheckinRecord> list = reservationService.listSeated();
        seatedModel.setRowCount(0);
        for (CheckinRecord record : list) {
            Reservation reservation = reservationByNo.get(record.getResNo());
            seatedModel.addRow(new Object[]{
                    record.getRecordNo(), record.getResNo(),
                    reservation != null && reservation.getSeatNo() != null ? reservation.getSeatNo() : "--",
                    reservation != null && reservation.getStudentName() != null ? reservation.getStudentName() : "--",
                    reservation != null && reservation.getPhone() != null ? reservation.getPhone() : "--",
                    SDF_DATETIME.format(record.getActualStart()),
                    record.getPenalty() != null ? record.getPenalty() : 0
            });
        }
    }

    // ==================== 通用 ====================

    private void switchTab(String card) {
        currentCard = card;
        cardLayout.show(centerPanel, card);
        UiTheme.activateTab(bottomTabBar, "all".equals(card) ? 0 : 1);
        if ("all".equals(card)) {
            refreshAllResv();
        } else {
            refreshSeated();
        }
    }

    private void refreshAll() {
        refreshAllResv();
        refreshSeated();
    }

    private static LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
