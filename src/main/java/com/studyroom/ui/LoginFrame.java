package com.studyroom.ui;

import com.studyroom.entity.User;
import com.studyroom.service.AuthService;
import com.studyroom.service.BusinessException;
import com.studyroom.service.ServiceFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URL;

/**
 * 登录窗口：左半侧展示自习室实景图，右半侧登录表单。
 */
public class LoginFrame extends JFrame {

    private final AuthService authService;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;

    public LoginFrame() {
        authService = ServiceFactory.getAuthService();

        setTitle("自习室预约管理系统 - 登录");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(820, 500);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(new RoomImagePanel(loadRoomImage()), BorderLayout.WEST);
        mainPanel.add(createFormPanel(), BorderLayout.CENTER);
        setContentPane(mainPanel);
        getRootPane().setDefaultButton(loginButton);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                usernameField.requestFocusInWindow();
            }
        });
    }

    /** 优先从 classpath 读取（打包后可随 jar 一起分发），再回退到仓库内的图片。 */
    private Image loadRoomImage() {
        URL resource = LoginFrame.class.getResource("/image.jpg");
        if (resource != null) {
            return new ImageIcon(resource).getImage();
        }
        File file = new File("docs/assets/image.jpg");
        return file.exists() ? new ImageIcon(file.getAbsolutePath()).getImage() : null;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(40, 42, 40, 42));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 0);

        JLabel titleLabel = new JLabel("欢迎登录", SwingConstants.CENTER);
        titleLabel.setFont(UiTheme.font(Font.BOLD, 24));
        titleLabel.setForeground(UiTheme.TEXT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        JLabel subLabel = new JLabel("自习室预约管理系统", SwingConstants.CENTER);
        subLabel.setFont(UiTheme.font(Font.PLAIN, 13));
        subLabel.setForeground(new Color(158, 158, 158));
        gbc.gridy = 1;
        gbc.insets = new Insets(6, 0, 24, 0);
        panel.add(subLabel, gbc);

        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(8, 0, 4, 8);
        panel.add(fieldLabel("用户名"), gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(8, 0, 4, 0);
        usernameField = new JTextField(15);
        usernameField.setFont(UiTheme.font(Font.PLAIN, 14));
        usernameField.setBorder(inputBorder());
        panel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.insets = new Insets(4, 0, 4, 8);
        panel.add(fieldLabel("密码"), gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(4, 0, 4, 0);
        passwordField = new JPasswordField(15);
        passwordField.setFont(UiTheme.font(Font.PLAIN, 14));
        passwordField.setBorder(inputBorder());
        panel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(22, 0, 6, 0);
        loginButton = UiTheme.primaryButton("登   录", UiTheme.BLUE, e -> doLogin());
        loginButton.setFont(UiTheme.font(Font.BOLD, 15));
        loginButton.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        panel.add(loginButton, gbc);

        gbc.gridy = 5;
        gbc.insets = new Insets(14, 0, 0, 0);
        JLabel hintLabel = new JLabel("学生：学号登录　|　管理员：duty / admin", SwingConstants.CENTER);
        hintLabel.setFont(UiTheme.font(Font.PLAIN, 11));
        hintLabel.setForeground(new Color(158, 158, 158));
        panel.add(hintLabel, gbc);

        gbc.gridy = 6;
        gbc.insets = new Insets(2, 0, 0, 0);
        JLabel pwdHint = new JLabel("初始密码均为 123456", SwingConstants.CENTER);
        pwdHint.setFont(UiTheme.font(Font.PLAIN, 11));
        pwdHint.setForeground(new Color(158, 158, 158));
        panel.add(pwdHint, gbc);

        return panel;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UiTheme.font(Font.PLAIN, 13));
        label.setForeground(UiTheme.GREY);
        return label;
    }

    private javax.swing.border.Border inputBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 189, 189), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10));
    }

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入用户名和密码", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            User user = authService.login(username, password);
            openMainFrame(user);
        } catch (BusinessException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "登录失败", JOptionPane.WARNING_MESSAGE);
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        }
    }

    private void openMainFrame(User user) {
        JFrame mainFrame;
        switch (user.getRole()) {
            case STUDENT:
                mainFrame = new StudentMainFrame(user);
                break;
            case DUTY_ADMIN:
                mainFrame = new DutyAdminMainFrame(user);
                break;
            case SYS_ADMIN:
                mainFrame = new SysAdminMainFrame(user);
                break;
            default:
                JOptionPane.showMessageDialog(this, "未知角色：" + user.getRole(), "错误", JOptionPane.ERROR_MESSAGE);
                return;
        }
        mainFrame.setVisible(true);
        this.dispose();
    }

    /** 按“铺满裁切”方式绘制图片，避免固定尺寸拉伸变形；底部叠加渐变保证文字可读。 */
    private static final class RoomImagePanel extends JPanel {
        private final Image image;

        RoomImagePanel(Image image) {
            this.image = image;
            setPreferredSize(new Dimension(400, 500));
            setBackground(new Color(30, 30, 30));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int width = getWidth();
            int height = getHeight();
            if (image != null && image.getWidth(null) > 0 && image.getHeight(null) > 0) {
                double scale = Math.max(width / (double) image.getWidth(null),
                        height / (double) image.getHeight(null));
                int scaledWidth = (int) Math.round(image.getWidth(null) * scale);
                int scaledHeight = (int) Math.round(image.getHeight(null) * scale);
                g2.drawImage(image, (width - scaledWidth) / 2, (height - scaledHeight) / 2,
                        scaledWidth, scaledHeight, null);
            }

            int gradientTop = Math.max(0, height - 150);
            g2.setPaint(new GradientPaint(0, gradientTop, new Color(0, 0, 0, 0),
                    0, height, new Color(0, 0, 0, 180)));
            g2.fillRect(0, gradientTop, width, height - gradientTop);

            g2.setFont(UiTheme.font(Font.BOLD, 20));
            g2.setColor(Color.WHITE);
            String slogan = image == null ? "自习室预约系统" : "安静 · 专注 · 高效";
            FontMetrics metrics = g2.getFontMetrics();
            g2.drawString(slogan, (width - metrics.stringWidth(slogan)) / 2, height - 34);
            g2.dispose();
        }
    }
}
