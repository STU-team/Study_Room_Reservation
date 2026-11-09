package com.studyroom.ui;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * 全局视觉主题：颜色、字体、通用按钮与底部标签栏。
 * 三个角色窗口共用这一份，避免每个类各写一套样式后逐渐走样。
 */
public final class UiTheme {

    public static final Color HEADER = new Color(55, 71, 79);
    public static final Color BLUE = new Color(33, 150, 243);
    public static final Color GREEN = new Color(46, 125, 50);
    public static final Color RED = new Color(198, 40, 40);
    public static final Color ORANGE = new Color(230, 81, 0);
    public static final Color GREY = new Color(97, 97, 97);
    public static final Color SIDEBAR = new Color(240, 242, 245);
    public static final Color PANEL = new Color(245, 245, 250);
    public static final Color WHITE = Color.WHITE;
    public static final Color BORDER = new Color(224, 224, 224);
    public static final Color BORDER_DARK = new Color(200, 200, 200);
    public static final Color TEXT = new Color(55, 71, 79);
    public static final Color TEXT_MUTED = new Color(117, 117, 117);
    public static final Color TAB_INACTIVE = new Color(117, 117, 117);
    public static final Color SELECTED_BG = new Color(232, 240, 254);
    public static final Color TABLE_HEADER_BG = new Color(236, 239, 241);
    public static final Color ROW_LINE = new Color(238, 238, 238);

    public static final String FONT_FAMILY = "微软雅黑";

    private UiTheme() {
    }

    public static Font font(int style, int size) {
        return new Font(FONT_FAMILY, style, size);
    }

    public static void installGlobalFonts() {
        FontUIResource plain = new FontUIResource(font(Font.PLAIN, 13));
        String[] keys = {
                "Label.font", "Button.font", "TextField.font", "PasswordField.font",
                "ComboBox.font", "CheckBox.font", "RadioButton.font", "Spinner.font",
                "Table.font", "TableHeader.font", "TitledBorder.font",
                "OptionPane.messageFont", "OptionPane.buttonFont", "OptionPane.smallFont",
                "ToolTip.font", "Menu.font", "MenuItem.font"
        };
        for (String key : keys) {
            UIManager.put(key, plain);
        }
    }

    public static Color darken(Color color, double amount) {
        int r = (int) Math.round(color.getRed() * (1 - amount));
        int g = (int) Math.round(color.getGreen() * (1 - amount));
        int b = (int) Math.round(color.getBlue() * (1 - amount));
        return new Color(Math.max(0, r), Math.max(0, g), Math.max(0, b));
    }

    // ==================== 按钮 ====================

    public static JButton primaryButton(String text, Color color, ActionListener action) {
        FlatButton button = new FlatButton(text, color, Color.WHITE,
                darken(color, 0.12), darken(color, 0.22));
        button.addActionListener(action);
        return button;
    }

    public static JButton secondaryButton(String text, Color color, ActionListener action) {
        FlatButton button = new FlatButton(text, Color.WHITE, color,
                new Color(238, 245, 254), new Color(220, 233, 250));
        button.setOutline(color);
        button.addActionListener(action);
        return button;
    }

    public static JButton navButton(String text, Color color, ActionListener action) {
        FlatButton button = new FlatButton(text, color, Color.WHITE,
                darken(color, 0.12), darken(color, 0.22));
        button.setFont(font(Font.PLAIN, 12));
        button.setPadding(4, 14, 4, 14);
        button.addActionListener(action);
        return button;
    }

    public static JButton smallButton(String text, Color color, ActionListener action) {
        FlatButton button = new FlatButton(text, color, Color.WHITE,
                darken(color, 0.12), darken(color, 0.22));
        button.setFont(font(Font.PLAIN, 13));
        button.setPadding(5, 12, 5, 12);
        button.addActionListener(action);
        return button;
    }

    /** 侧栏按钮：白底描边，悬停变浅蓝，与内容区形成层次。 */
    public static JButton sideButton(String text, ActionListener action) {
        FlatButton button = new FlatButton(text, Color.WHITE, TEXT,
                new Color(232, 240, 254), new Color(214, 228, 248));
        button.setOutline(BORDER_DARK);
        button.setFont(font(Font.PLAIN, 14));
        button.setPadding(8, 10, 8, 10);
        Dimension size = new Dimension(120, 38);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.addActionListener(action);
        return button;
    }

    // ==================== 底部标签栏 ====================

    public static JButton tabButton(String text, ActionListener action) {
        TabButton button = new TabButton(text);
        button.addActionListener(action);
        return button;
    }

    public static JPanel tabBar(JButton... buttons) {
        JPanel bar = new JPanel(new GridLayout(1, buttons.length));
        bar.setBackground(WHITE);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));
        for (JButton button : buttons) {
            bar.add(button);
        }
        return bar;
    }

    public static void activateTab(JPanel tabBar, int index) {
        for (int i = 0; i < tabBar.getComponentCount(); i++) {
            if (tabBar.getComponent(i) instanceof TabButton button) {
                button.setActive(i == index);
            }
        }
    }

    // ==================== 自绘控件 ====================

    private static final class FlatButton extends JButton {
        private final Color fill;
        private final Color foreground;
        private final Color hoverFill;
        private final Color pressedFill;
        private Color outline;

        FlatButton(String text, Color fill, Color foreground, Color hoverFill, Color pressedFill) {
            super(text);
            this.fill = fill;
            this.foreground = foreground;
            this.hoverFill = hoverFill;
            this.pressedFill = pressedFill;
            setFont(font(Font.PLAIN, 13));
            setForeground(foreground);
            setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        void setOutline(Color color) {
            this.outline = color;
        }

        void setPadding(int top, int left, int bottom, int right) {
            setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color background;
            if (!isEnabled()) {
                background = new Color(224, 224, 224);
            } else if (getModel().isPressed()) {
                background = pressedFill;
            } else if (getModel().isRollover()) {
                background = hoverFill;
            } else {
                background = fill;
            }
            g2.setColor(background);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
            if (outline != null) {
                g2.setColor(outline);
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
            }
            String text = getText();
            if (text != null && !text.isEmpty()) {
                g2.setFont(getFont());
                FontMetrics metrics = g2.getFontMetrics();
                int x = (getWidth() - metrics.stringWidth(text)) / 2;
                int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
                g2.setColor(isEnabled() ? foreground : TEXT_MUTED);
                g2.drawString(text, x, y);
            }
            g2.dispose();
        }
    }

    private static final class TabButton extends JButton {
        private boolean active;

        TabButton(String text) {
            super(text);
            setFont(font(Font.PLAIN, 15));
            setForeground(TAB_INACTIVE);
            setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        void setActive(boolean active) {
            this.active = active;
            setFont(font(active ? Font.BOLD : Font.PLAIN, 15));
            setForeground(active ? BLUE : TAB_INACTIVE);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (getModel().isRollover() && !active) {
                g2.setColor(new Color(247, 249, 252));
            } else {
                g2.setColor(WHITE);
            }
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setFont(getFont());
            FontMetrics metrics = g2.getFontMetrics();
            String text = getText();
            int x = (getWidth() - metrics.stringWidth(text)) / 2;
            int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            g2.setColor(getForeground());
            g2.drawString(text, x, y);
            if (active) {
                g2.setColor(BLUE);
                g2.fillRect(0, getHeight() - 3, getWidth(), 3);
            }
            g2.dispose();
        }
    }
}
