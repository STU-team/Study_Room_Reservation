package com.studyroom.ui;

import com.studyroom.entity.CheckinRecord;
import com.studyroom.entity.Reservation;
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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 系统管理员：座位维护、全部预约、在座名单、学生信用分。 */
public class SysAdminMainFrame extends JFrame {

    private final User currentUser;
    private final SeatService seatService;
    private final ReservationService reservationService;
    private final StudentService studentService;
    private final AuthService authService;

    private DefaultTableModel seatMgrModel;
    private DefaultTableModel allResvModel;
    private DefaultTableModel seatedModel;
    private DefaultTableModel studentModel;
    private JTable seatMgrTable;
    private JTable allResvTable;
    private JTable seatedTable;
    private JTable studentTable;
    private JTextField searchField;
    private CardLayout cardLayout;
    private JPanel centerPanel;
    private JPanel bottomTabBar;

    private String currentCard = "seat";

    private static final String[] CARDS = {"seat", "all", "seated", "student"};
    private static final SimpleDateFormat SDF_DATETIME = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private static final SimpleDateFormat SDF_DATE = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat SDF_TIME = new SimpleDateFormat("HH:mm");

    public SysAdminMainFrame(User user) {
        this.currentUser = user;
        this.seatService = ServiceFactory.getSeatService();
        this.reservationService = ServiceFactory.getReservationService();
        this.studentService = ServiceFactory.getStudentService();
        this.authService = ServiceFactory.getAuthService();

        setTitle("自习室预约系统 - 系统管理员");
        setSize(1180, 760);
        setMinimumSize(new Dimension(1000, 660));
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
        centerPanel.add(createSeatMgrPanel(), "seat");
        centerPanel.add(createAllResvPanel(), "all");
        centerPanel.add(createSeatedPanel(), "seated");
        centerPanel.add(createStudentPanel(), "student");
        middle.add(centerPanel, BorderLayout.CENTER);
        add(middle, BorderLayout.CENTER);

        bottomTabBar = UiTheme.tabBar(
                UiTheme.tabButton("座位管理", e -> switchTab("seat")),
                UiTheme.tabButton("全部预约", e -> switchTab("all")),
                UiTheme.tabButton("在座名单", e -> switchTab("seated")),
                UiTheme.tabButton("学生信用分", e -> switchTab("student")));
        add(bottomTabBar, BorderLayout.SOUTH);

        switchTab("seat");
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

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(UiTheme.navButton("爽约扫描", UiTheme.ORANGE, e -> doNoShowScan()));
        JLabel userLabel = new JLabel(currentUser.getUsername() + "（系统管理员）");
        userLabel.setFont(UiTheme.font(Font.PLAIN, 13));
        userLabel.setForeground(new Color(255, 255, 255, 200));
        right.add(userLabel);
        right.add(UiTheme.navButton("退出", UiTheme.RED, e -> logout()));
        nav.add(right, BorderLayout.EAST);
        return nav;
    }

    private void doNoShowScan() {
        try {
            int count = reservationService.handleNoShow(LocalDateTime.now());
            JOptionPane.showMessageDialog(this, "本次处理 " + count + " 条爽约记录",
                    "爽约扫描", JOptionPane.INFORMATION_MESSAGE);
            refreshAll();
        } catch (BusinessException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "扫描失败", JOptionPane.WARNING_MESSAGE);
        }
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

        JLabel label = new JLabel("管理操作");
        label.setFont(UiTheme.font(Font.BOLD, 14));
        label.setForeground(UiTheme.HEADER);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(label);
        sidebar.add(Box.createVerticalStrut(12));

        sidebar.add(UiTheme.sideButton("修改座位状态", e -> doChangeSeatStatus()));
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(UiTheme.sideButton("信用分明细", e -> doViewCreditSelected()));
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(UiTheme.sideButton("刷新全部", e -> refreshAll()));

        sidebar.add(Box.createVerticalStrut(16));
        JLabel hint = new JLabel("<html><body style='width:110px'>先选中表格记录，<br>再点左侧操作</body></html>");
        hint.setFont(UiTheme.font(Font.PLAIN, 11));
        hint.setForeground(UiTheme.TEXT_MUTED);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(hint);
        return sidebar;
    }

    private void ensureTab(String card) {
        if (!card.equals(currentCard)) {
            switchTab(card);
        }
    }

    // ==================== 座位管理 ====================

    private JPanel createSeatMgrPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UiTheme.PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
        panel.add(createSeatHintBar(), BorderLayout.NORTH);

        seatMgrModel = new DefaultTableModel(new String[]{"座位号", "区域", "状态"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        seatMgrTable = TableFactory.create(seatMgrModel, 48, "暂无座位数据");
        seatMgrTable.getColumnModel().getColumn(2).setCellRenderer(TableFactory.seatStatusRenderer());
        seatMgrTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    doChangeSeatStatus();
                }
            }
        });
        panel.add(TableFactory.scroll(seatMgrTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSeatHintBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 7));
        bar.setBackground(UiTheme.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDER),
                new EmptyBorder(0, 8, 0, 8)));

        JLabel hint = new JLabel("选中座位后可修改状态，双击也可修改");
        hint.setFont(UiTheme.font(Font.PLAIN, 13));
        hint.setForeground(UiTheme.TEXT_MUTED);
        bar.add(hint);

        for (SeatStatus status : SeatStatus.values()) {
            JLabel item = new JLabel("● " + TableFactory.seatName(status));
            item.setFont(UiTheme.font(Font.PLAIN, 13));
            item.setForeground(TableFactory.seatColor(status));
            bar.add(item);
        }
        return bar;
    }

    private void refreshSeatMgr() {
        seatMgrModel.setRowCount(0);
        for (Seat seat : seatService.listAllSeats()) {
            seatMgrModel.addRow(new Object[]{seat.getSeatNo(), seat.getRoomName(), seat.getStatus()});
        }
    }

    private void doChangeSeatStatus() {
        ensureTab("seat");
        int row = seatMgrTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "请先在“座位管理”中选中一个座位",
                    "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String seatNo = String.valueOf(seatMgrTable.getValueAt(row, 0));
        String roomName = String.valueOf(seatMgrTable.getValueAt(row, 1));
        SeatStatus current = (SeatStatus) seatMgrTable.getValueAt(row, 2);

        SeatStatus[] candidates = new SeatStatus[SeatStatus.values().length - 1];
        int index = 0;
        for (SeatStatus status : SeatStatus.values()) {
            if (status != current) {
                candidates[index++] = status;
            }
        }
        JComboBox<SeatStatus> combo = new JComboBox<>(candidates);
        combo.setFont(UiTheme.font(Font.PLAIN, 14));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof SeatStatus status) {
                    setText(TableFactory.seatName(status));
                }
                return this;
            }
        });

        JPanel input = new JPanel(new GridLayout(0, 1, 0, 8));
        input.setBorder(new EmptyBorder(8, 8, 4, 8));
        input.add(new JLabel("座位：" + seatNo + "（" + roomName + "）"));
        input.add(new JLabel("当前状态：" + TableFactory.seatName(current)));
        JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        line.setOpaque(false);
        line.add(new JLabel("修改为："));
        line.add(combo);
        input.add(line);

        int confirm = JOptionPane.showConfirmDialog(this, input, "修改座位状态",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (confirm != JOptionPane.OK_OPTION) {
            return;
        }
        SeatStatus target = (SeatStatus) combo.getSelectedItem();
        if (target == null || target == current) {
            return;
        }
        try {
            if (target == SeatStatus.CLOSED) {
                if (current == SeatStatus.RESERVED
                        && JOptionPane.showConfirmDialog(this,
                        "座位 " + seatNo + " 当前为预约状态，转维修会取消其全部未完成预约，是否继续？",
                        "确认转维修", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                    return;
                }
                int handled = reservationService.closeSeatForRepair(seatNo);
                String msg = handled == 0
                        ? "座位 " + seatNo + " 已转维修"
                        : "已处理 " + handled + " 条未完成预约，座位 " + seatNo + " 已转维修";
                JOptionPane.showMessageDialog(this, msg);
            } else {
                seatService.updateSeatStatus(seatNo, target);
                JOptionPane.showMessageDialog(this, "座位 " + seatNo + " 已改为「"
                        + TableFactory.seatName(target) + "」");
            }
            refreshAll();
        } catch (BusinessException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "修改失败", JOptionPane.WARNING_MESSAGE);
            refreshSeatMgr();
        }
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

    // ==================== 学生信用分 ====================

    private JPanel createStudentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UiTheme.PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));

        JPanel hintBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 7));
        hintBar.setBackground(UiTheme.WHITE);
        hintBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDER),
                new EmptyBorder(0, 12, 0, 12)));
        JLabel hint = new JLabel("信用分低于 80 分将暂停预约资格；双击学生可查看明细");
        hint.setFont(UiTheme.font(Font.PLAIN, 13));
        hint.setForeground(UiTheme.TEXT_MUTED);
        hintBar.add(hint);
        panel.add(hintBar, BorderLayout.NORTH);

        String[] columns = {"学号", "姓名", "手机", "信用分"};
        studentModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        studentTable = TableFactory.create(studentModel, 44, "暂无学生数据");
        studentTable.getColumnModel().getColumn(3).setCellRenderer(creditRenderer());
        studentTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && studentTable.getSelectedRow() != -1) {
                    doViewCredit(String.valueOf(studentTable.getValueAt(studentTable.getSelectedRow(), 0)));
                }
            }
        });
        panel.add(TableFactory.scroll(studentTable), BorderLayout.CENTER);
        return panel;
    }

    private DefaultTableCellRenderer creditRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int score = parseInt(value);
                setText(value == null ? "" : value.toString());
                setHorizontalAlignment(SwingConstants.CENTER);
                setBackground(isSelected ? UiTheme.SELECTED_BG : UiTheme.WHITE);
                setForeground(score < 80 ? UiTheme.RED : UiTheme.TEXT);
                setFont(UiTheme.font(score < 80 ? Font.BOLD : Font.PLAIN, 15));
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.ROW_LINE),
                        BorderFactory.createEmptyBorder(2, 8, 2, 8)));
                return this;
            }
        };
    }

    private static int parseInt(Object value) {
        try {
            return value == null ? 0 : Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void refreshStudents() {
        studentModel.setRowCount(0);
        for (Student student : studentService.listStudents()) {
            studentModel.addRow(new Object[]{
                    student.getStudentId(), student.getName(), student.getPhone(), student.getCreditScore()
            });
        }
    }

    private void doViewCreditSelected() {
        ensureTab("student");
        int row = studentTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "请先在“学生信用分”中选中一名学生",
                    "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        doViewCredit(String.valueOf(studentTable.getValueAt(row, 0)));
    }

    /** 信用分由业务流水自动增减，这里只做只读查询，不提供手工改分。 */
    private void doViewCredit(String studentId) {
        Student student;
        try {
            student = studentService.findByStudentId(studentId);
        } catch (BusinessException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "查询失败", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int score = student.getCreditScore() == null ? 0 : student.getCreditScore();

        JPanel info = new JPanel(new GridBagLayout());
        info.setBorder(new EmptyBorder(10, 14, 6, 14));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.LINE_START;
        gc.gridy = 0;
        addInfoRow(info, gc, 0, "学号", student.getStudentId());
        addInfoRow(info, gc, 1, "姓名", student.getName());
        addInfoRow(info, gc, 2, "手机", student.getPhone());

        gc.gridx = 0;
        gc.gridy = 3;
        gc.weightx = 0;
        JLabel scoreTitle = new JLabel("当前信用分：");
        scoreTitle.setFont(UiTheme.font(Font.PLAIN, 14));
        info.add(scoreTitle, gc);

        gc.gridx = 1;
        gc.weightx = 1;
        JLabel scoreValue = new JLabel(score + " 分");
        scoreValue.setFont(UiTheme.font(Font.BOLD, 18));
        scoreValue.setForeground(score < 80 ? UiTheme.RED : UiTheme.GREEN);
        info.add(scoreValue, gc);

        addInfoRow(info, gc, 4, "预约资格", score < 80 ? "已暂停（信用分低于 80）" : "正常");

        JLabel rules = new JLabel("<html><body style='width:250px'>"
                + "扣分规则：<br>"
                + "· 爽约：-5 分<br>"
                + "· 超时签退：-1 分<br><br>"
                + "信用分由系统在签退和爽约扫描时自动结算，不支持手工修改。"
                + "</body></html>");
        rules.setFont(UiTheme.font(Font.PLAIN, 12));
        rules.setForeground(UiTheme.TEXT_MUTED);
        rules.setBorder(new EmptyBorder(4, 18, 4, 18));
        info.add(rules, fullWidthRow(5));

        JOptionPane.showMessageDialog(this, info,
                "信用分明细 - " + student.getName(), JOptionPane.INFORMATION_MESSAGE);
    }

    private static GridBagConstraints fullWidthRow(int row) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = row;
        gc.gridx = 0;
        gc.gridwidth = 2;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.LINE_START;
        return gc;
    }

    private static void addInfoRow(JPanel panel, GridBagConstraints gc, int row, String label, String value) {
        gc.gridx = 0;
        gc.gridy = row;
        gc.weightx = 0;
        gc.gridwidth = 1;
        gc.anchor = GridBagConstraints.LINE_START;
        JLabel nameLabel = new JLabel(label + "：");
        nameLabel.setFont(UiTheme.font(Font.PLAIN, 14));
        nameLabel.setForeground(UiTheme.TEXT_MUTED);
        panel.add(nameLabel, gc);

        gc.gridx = 1;
        gc.weightx = 1;
        JLabel valueLabel = new JLabel(value == null || value.isBlank() ? "--" : value);
        valueLabel.setFont(UiTheme.font(Font.PLAIN, 14));
        panel.add(valueLabel, gc);
    }

    // ==================== 通用 ====================

    private void switchTab(String card) {
        currentCard = card;
        cardLayout.show(centerPanel, card);
        for (int i = 0; i < CARDS.length; i++) {
            if (CARDS[i].equals(card)) {
                UiTheme.activateTab(bottomTabBar, i);
                break;
            }
        }
        switch (card) {
            case "seat" -> refreshSeatMgr();
            case "all" -> refreshAllResv();
            case "seated" -> refreshSeated();
            case "student" -> refreshStudents();
            default -> {
            }
        }
    }

    private void refreshAll() {
        refreshSeatMgr();
        refreshAllResv();
        refreshSeated();
        refreshStudents();
    }
}
