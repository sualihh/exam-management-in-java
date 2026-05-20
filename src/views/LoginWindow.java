package views;

import services.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Login window — split layout:
 *   Left  420px orange branding panel
 *   Right dark panel with username/password form
 */
public class LoginWindow extends JFrame {

    private JTextField     txtUsername;
    private JPasswordField txtPassword;
    private JLabel         lblError;

    public LoginWindow() {
        super("ExamPlatform — Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(860, 540);
        setMinimumSize(new Dimension(760, 480));
        setLocationRelativeTo(null);
        setResizable(false);
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);

        // ── Left branding panel ───────────────────────────────────────────────
        JPanel left = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Gradient: orange top → darker orange bottom
                GradientPaint gp = new GradientPaint(0, 0, Theme.ACCENT, 0, getHeight(), Theme.ACCENT_DARK);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        left.setPreferredSize(new Dimension(420, 540));
        left.setLayout(new GridBagLayout());

        JPanel brandBox = new JPanel();
        brandBox.setOpaque(false);
        brandBox.setLayout(new BoxLayout(brandBox, BoxLayout.Y_AXIS));

        JLabel icon = new JLabel("📋");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 64));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("ExamPlatform");
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Online Examination System");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(new Color(255, 255, 255, 180));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        brandBox.add(icon);
        brandBox.add(Box.createVerticalStrut(16));
        brandBox.add(title);
        brandBox.add(Box.createVerticalStrut(8));
        brandBox.add(sub);

        left.add(brandBox);

        // ── Right form panel ──────────────────────────────────────────────────
        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(Theme.BG);
        right.setBorder(new EmptyBorder(40, 50, 40, 50));

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setMaximumSize(new Dimension(340, 400));

        JLabel welcome = new JLabel("Welcome back");
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 26));
        welcome.setForeground(Color.WHITE);
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel hint = new JLabel("Sign in to your account");
        hint.setFont(Theme.FONT_BODY);
        hint.setForeground(Theme.TEXT_MUTED);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(welcome);
        form.add(Box.createVerticalStrut(4));
        form.add(hint);
        form.add(Box.createVerticalStrut(32));

        // Username
        JLabel lblUser = Theme.label("Username");
        lblUser.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtUsername = Theme.inputField();
        txtUsername.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtUsername.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(lblUser);
        form.add(Box.createVerticalStrut(6));
        form.add(txtUsername);
        form.add(Box.createVerticalStrut(16));

        // Password
        JLabel lblPass = Theme.label("Password");
        lblPass.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtPassword = Theme.passwordField();
        txtPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtPassword.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(lblPass);
        form.add(Box.createVerticalStrut(6));
        form.add(txtPassword);
        form.add(Box.createVerticalStrut(8));

        // Error label
        lblError = Theme.errorLabel();
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblError);
        form.add(Box.createVerticalStrut(16));

        // Login button
        JButton btnLogin = Theme.primaryButton("SIGN IN");
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        form.add(btnLogin);

        right.add(form);

        root.add(left,  BorderLayout.WEST);
        root.add(right, BorderLayout.CENTER);
        setContentPane(root);

        // ── Events ────────────────────────────────────────────────────────────
        btnLogin.addActionListener(e -> attemptLogin());

        KeyAdapter enterKey = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) attemptLogin();
            }
        };
        txtUsername.addKeyListener(enterKey);
        txtPassword.addKeyListener(enterKey);

        SwingUtilities.invokeLater(() -> txtUsername.requestFocusInWindow());
    }

    private void attemptLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter your username and password.");
            return;
        }

        try {
            boolean success = AuthService.login(username, password);
            if (!success) {
                showError("Invalid username or password.");
                txtPassword.setText("");
                return;
            }
        } catch (Exception ex) {
            showError("Database error: " + ex.getMessage());
            return;
        }

        lblError.setText(" ");

        // If first login, force password change
        if (AuthService.getCurrentUser().isMustChangePassword()) {
            ChangePasswordDialog dlg = new ChangePasswordDialog(this);
            dlg.setVisible(true);
        }

        // Route to the correct dashboard based on role
        String role = AuthService.getCurrentUser().getRoleName();
        JFrame next;
        switch (role) {
            case "Admin":   next = new AdminDashboard();   break;
            case "Teacher": next = new TeacherDashboard(); break;
            case "Student": next = new StudentDashboard(); break;
            default:
                showError("Unknown role: " + role);
                return;
        }

        next.setVisible(true);
        dispose();
    }

    private void showError(String msg) {
        lblError.setText(msg);
    }
}
