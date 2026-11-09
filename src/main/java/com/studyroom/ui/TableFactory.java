package com.studyroom.ui;

import com.studyroom.entity.ReservationStatus;
import com.studyroom.entity.SeatStatus;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 表格统一工厂：表头、行高、选中态、空数据提示、状态徽章和排序。
 * 状态列请显式套用 {@link #seatStatusRenderer()} / {@link #reservationStatusRenderer()}。
 */
public final class TableFactory {

    private static final Map<SeatStatus, String> SEAT_NAMES = new EnumMap<>(SeatStatus.class);
    private static final Map<SeatStatus, Color> SEAT_BG = new EnumMap<>(SeatStatus.class);
    private static final Map<SeatStatus, Color> SEAT_FG = new EnumMap<>(SeatStatus.class);

    private static final Map<ReservationStatus, String> RESV_NAMES = new EnumMap<>(ReservationStatus.class);
    private static final Map<ReservationStatus, Color> RESV_BG = new EnumMap<>(ReservationStatus.class);
    private static final Map<ReservationStatus, Color> RESV_FG = new EnumMap<>(ReservationStatus.class);

    /** 单元格左右内边距合计，用于判断文字是否超出可用宽度。 */
    private static final int CELL_PADDING = 16;

    static {
        putSeat(SeatStatus.AVAILABLE, "可用", new Color(232, 245, 233), new Color(46, 125, 50));
        putSeat(SeatStatus.RESERVED, "已预约", new Color(255, 243, 224), new Color(230, 81, 0));
        putSeat(SeatStatus.OCCUPIED, "使用中", new Color(255, 235, 238), new Color(198, 40, 40));
        putSeat(SeatStatus.CLOSED, "维修", new Color(245, 245, 245), new Color(97, 97, 97));

        putResv(ReservationStatus.BOOKED, "待签到", new Color(227, 242, 253), new Color(21, 101, 192));
        putResv(ReservationStatus.CHECKED_IN, "使用中", new Color(232, 245, 233), new Color(46, 125, 50));
        putResv(ReservationStatus.CHECKED_OUT, "已签退", new Color(245, 245, 245), new Color(97, 97, 97));
        putResv(ReservationStatus.CANCELLED, "已取消", new Color(250, 250, 250), new Color(158, 158, 158));
        putResv(ReservationStatus.NO_SHOW, "爽约", new Color(255, 235, 238), new Color(198, 40, 40));
    }

    private TableFactory() {
    }

    private static void putSeat(SeatStatus status, String name, Color bg, Color fg) {
        SEAT_NAMES.put(status, name);
        SEAT_BG.put(status, bg);
        SEAT_FG.put(status, fg);
    }

    private static void putResv(ReservationStatus status, String name, Color bg, Color fg) {
        RESV_NAMES.put(status, name);
        RESV_BG.put(status, bg);
        RESV_FG.put(status, fg);
    }

    public static JTable create(DefaultTableModel model, int rowHeight) {
        return create(model, rowHeight, "暂无数据");
    }

    public static JTable create(DefaultTableModel model, int rowHeight, String emptyText) {
        EmptyStateTable table = new EmptyStateTable(emptyText);
        table.setModel(model);
        table.setRowHeight(rowHeight);
        table.setFont(UiTheme.font(Font.PLAIN, 15));
        table.setForeground(UiTheme.TEXT);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(UiTheme.SELECTED_BG);
        table.setSelectionForeground(UiTheme.TEXT);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setAutoCreateRowSorter(true);
        table.setDefaultRenderer(Object.class, textRenderer());

        JTableHeader header = table.getTableHeader();
        header.setFont(UiTheme.font(Font.BOLD, 14));
        header.setBackground(UiTheme.TABLE_HEADER_BG);
        header.setForeground(UiTheme.TEXT);
        header.setPreferredSize(new Dimension(0, 38));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new HeaderRenderer());
        return table;
    }

    public static JScrollPane scroll(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    public static void setEmptyText(JTable table, String text) {
        if (table instanceof EmptyStateTable emptyStateTable) {
            emptyStateTable.setEmptyText(text);
        }
    }

    public static DefaultTableCellRenderer seatStatusRenderer() {
        return statusRenderer(
                value -> value instanceof SeatStatus status ? SEAT_NAMES.get(status) : textOf(value),
                value -> value instanceof SeatStatus status ? SEAT_BG.get(status) : Color.WHITE,
                value -> value instanceof SeatStatus status ? SEAT_FG.get(status) : UiTheme.TEXT);
    }

    public static DefaultTableCellRenderer reservationStatusRenderer() {
        return statusRenderer(
                value -> value instanceof ReservationStatus status ? RESV_NAMES.get(status) : textOf(value),
                value -> value instanceof ReservationStatus status ? RESV_BG.get(status) : Color.WHITE,
                value -> value instanceof ReservationStatus status ? RESV_FG.get(status) : UiTheme.TEXT);
    }

    public static String seatName(SeatStatus status) {
        return status == null ? "--" : SEAT_NAMES.getOrDefault(status, status.name());
    }

    public static String reservationName(ReservationStatus status) {
        return status == null ? "--" : RESV_NAMES.getOrDefault(status, status.name());
    }

    public static Color seatColor(SeatStatus status) {
        return status == null ? UiTheme.GREY : SEAT_FG.getOrDefault(status, UiTheme.GREY);
    }

    private static DefaultTableCellRenderer textRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String text = textOf(value);
                setFont(UiTheme.font(Font.PLAIN, 15));
                int available = table.getColumnModel().getColumn(column).getWidth() - CELL_PADDING;
                setText(wrap(text, getFontMetrics(getFont()), available));
                setHorizontalAlignment(SwingConstants.CENTER);
                setBackground(isSelected ? UiTheme.SELECTED_BG : Color.WHITE);
                setForeground(UiTheme.TEXT);
                setBorder(cellBorder());
                setToolTipText(isClipped(getFontMetrics(getFont()), text, available) ? text : null);
                return this;
            }
        };
    }

    /**
     * 超出列宽的文字拆成两行：日期时间优先在空格处断行，其余内容取最均衡的断点，
     * 预约号、手机号这类没有空格的字段也能完整显示。
     */
    private static String wrap(String text, FontMetrics metrics, int available) {
        if (text.isEmpty() || available <= 0 || metrics.stringWidth(text) <= available) {
            return text;
        }
        int split = findSplit(text, metrics, available);
        if (split <= 0 || split >= text.length()) {
            return text;
        }
        return "<html><center>" + escape(text.substring(0, split).trim())
                + "<br>" + escape(text.substring(split).trim()) + "</center></html>";
    }

    private static int findSplit(String text, FontMetrics metrics, int available) {
        int space = text.indexOf(' ');
        if (space > 0
                && metrics.stringWidth(text.substring(0, space)) <= available
                && metrics.stringWidth(text.substring(space + 1)) <= available) {
            return space + 1;
        }
        int best = -1;
        int bestBalance = Integer.MAX_VALUE;
        for (int i = 1; i < text.length(); i++) {
            int left = metrics.stringWidth(text.substring(0, i));
            int right = metrics.stringWidth(text.substring(i));
            if (left <= available && right <= available) {
                int balance = Math.abs(left - right);
                if (balance < bestBalance) {
                    bestBalance = balance;
                    best = i;
                }
            }
        }
        return best;
    }

    private static boolean isClipped(FontMetrics metrics, String text, int available) {
        return !text.isEmpty() && metrics.stringWidth(text) > available;
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static DefaultTableCellRenderer statusRenderer(Function<Object, String> textMapper,
                                                           Function<Object, Color> backgroundMapper,
                                                           Function<Object, Color> foregroundMapper) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setText(textMapper.apply(value));
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(UiTheme.font(Font.BOLD, 14));
                Color background = backgroundMapper.apply(value);
                setBackground(isSelected ? UiTheme.darken(background, 0.06) : background);
                setForeground(foregroundMapper.apply(value));
                setBorder(cellBorder());
                return this;
            }
        };
    }

    private static javax.swing.border.Border cellBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.ROW_LINE),
                BorderFactory.createEmptyBorder(2, 8, 2, 8));
    }

    private static String textOf(Object value) {
        return value == null ? "" : value.toString();
    }

    private static final class EmptyStateTable extends JTable {
        private String emptyText;

        EmptyStateTable(String emptyText) {
            this.emptyText = emptyText;
        }

        void setEmptyText(String emptyText) {
            this.emptyText = emptyText;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getRowCount() > 0 || emptyText == null || emptyText.isEmpty()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(UiTheme.TEXT_MUTED);
            g2.setFont(UiTheme.font(Font.PLAIN, 14));
            FontMetrics metrics = g2.getFontMetrics();
            int x = Math.max(8, (getWidth() - metrics.stringWidth(emptyText)) / 2);
            int y = Math.max(48, getHeight() / 2);
            g2.drawString(emptyText, x, y);
            g2.dispose();
        }
    }

    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setText(textOf(value));
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(UiTheme.font(Font.BOLD, 14));
            setBackground(UiTheme.TABLE_HEADER_BG);
            setForeground(UiTheme.TEXT);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDER),
                    BorderFactory.createEmptyBorder(0, 6, 0, 6)));
            setIcon(null);
            setIconTextGap(4);
            JTableHeader header = table.getTableHeader();
            if (header != null && header.getTable() != null) {
                RowSorter<?> sorter = table.getRowSorter();
                int modelColumn = table.convertColumnIndexToModel(column);
                if (sorter != null && modelColumn >= 0) {
                    List<? extends RowSorter.SortKey> keys = sorter.getSortKeys();
                    for (RowSorter.SortKey key : keys) {
                        if (key.getColumn() == modelColumn) {
                            setIcon(new SortArrowIcon(key.getSortOrder() == SortOrder.ASCENDING));
                            break;
                        }
                    }
                }
            }
            return this;
        }
    }

    private static final class SortArrowIcon implements Icon {
        private final boolean ascending;

        SortArrowIcon(boolean ascending) {
            this.ascending = ascending;
        }

        @Override
        public int getIconWidth() {
            return 8;
        }

        @Override
        public int getIconHeight() {
            return 8;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(UiTheme.BLUE);
            int[] xs = {x, x + 8, x + 4};
            int[] ys = ascending ? new int[]{y + 6, y + 6, y} : new int[]{y, y, y + 6};
            g2.fillPolygon(xs, ys, 3);
            g2.dispose();
        }
    }
}
